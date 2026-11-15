package com.visionmod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OPTIONAL, paired, loopback-only browser metadata receiver.
 *
 * Never scans microphone/audio, never receives cookies, never posts metadata
 * to a user-controlled server. A browser extension sends just title/artist/
 * position from Spotify or SoundCloud. LRCLIB lyric lookup is asynchronous,
 * rate-limited, and occurs only while the user has enabled World Lyrics.
 */
public final class VisionBrowserMusicBridge {
    public static final int PORT=38567;
    private static final Path CONFIG=FabricLoader.getInstance().getConfigDir()
        .resolve("visionmod/browser-bridge.properties");
    private static HttpServer server;
    private static ExecutorService executor;
    private static String token;
    private static long previousSearchAt;
    private static String lastSearched="";
    private static boolean searchPending;
    private static final HttpClient HTTP=HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
    private VisionBrowserMusicBridge(){}

    public static void init(){
        token=readToken();
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            boolean required=VisionModClient.enabled && VisionExtendedFeatures.enabled(3);
            if(required && server==null)start();
            else if(!required && server!=null)stop();
        });
    }
    public static String tokenFile(){return CONFIG.toString();}
    public static boolean connected(){return VisionLyricsSource.browserConnected();}
    public static String state(){return server==null?"OFF":connected()?"CONNECTED":"WAITING FOR BROWSER";}

    private static String readToken(){
        Properties p=new Properties();
        try{
            if(Files.isRegularFile(CONFIG)) {
                try(InputStream in=Files.newInputStream(CONFIG)){p.load(in);}
                String v=p.getProperty("token","");
                if(v.matches("[0-9a-f]{32}"))return v;
            }
            byte[] random=new byte[16];
            new SecureRandom().nextBytes(random);
            StringBuilder b=new StringBuilder(32);
            for(byte x:random)b.append(String.format("%02x",x&255));
            p.setProperty("token",b.toString());
            Files.createDirectories(CONFIG.getParent());
            try(OutputStream out=Files.newOutputStream(CONFIG)){
                p.store(out,"Paste this local token into Vision browser extension options; keep private");
            }
            return b.toString();
        }catch(Exception e){
            System.err.println("[VisionMod] Browser pairing token unavailable: "+e);
            return null; // no unpaired / unauthenticated service allowed
        }
    }

    private static void start(){
        if(token==null)return;
        try{
            server=HttpServer.create(new InetSocketAddress(
                InetAddress.getByName("127.0.0.1"),PORT),0);
            executor=Executors.newSingleThreadExecutor(r->{
                Thread t=new Thread(r,"VisionMod-BrowserBridge");
                t.setDaemon(true);return t;
            });
            server.setExecutor(executor);
            server.createContext("/track",VisionBrowserMusicBridge::handle);
            server.start();
            System.out.println("[VisionMod] Browser bridge bound to 127.0.0.1:"+PORT+
                " (paired extension only)");
        }catch(Exception e){
            server=null;
            if(executor!=null){executor.shutdownNow();executor=null;}
            System.err.println("[VisionMod] Browser bridge not started: "+e);
        }
    }
    private static void stop(){
        if(server!=null)server.stop(0);
        server=null;
        if(executor!=null)executor.shutdownNow();
        executor=null;
        VisionLyricsSource.clearBrowserTrack();
    }

    private static void handle(HttpExchange exchange){
        try{
            String origin=exchange.getRequestHeaders().getFirst("Origin");
            // Chrome extension background fetch may not send an Origin header.
            // A regular webpage is not trusted, even on localhost.
            if(origin!=null &&
                !(origin.startsWith("chrome-extension://")||origin.startsWith("moz-extension://"))) {
                respond(exchange,403,"Forbidden");
                return;
            }
            if(origin!=null) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin",origin);
                exchange.getResponseHeaders().set("Vary","Origin");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers","X-Vision-Token, Content-Type");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods","POST, OPTIONS");
            }
            if(exchange.getRequestMethod().equals("OPTIONS")){respond(exchange,204,"");return;}
            if(!exchange.getRequestMethod().equals("POST") ||
                !token.equals(exchange.getRequestHeaders().getFirst("X-Vision-Token"))){
                respond(exchange,403,"Forbidden");return;
            }
            int len=0;
            try{len=Integer.parseInt(exchange.getRequestHeaders().getFirst("Content-Length"));}
            catch(Exception ignored){}
            if(len>2048){respond(exchange,413,"Too large");return;}
            byte[] bytes;
            try(InputStream in=exchange.getRequestBody()){bytes=in.readNBytes(2049);}
            if(bytes.length>2048){respond(exchange,413,"Too large");return;}
            JsonObject body=JsonParser.parseString(
                new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
            String provider=text(body,"provider",30),title=text(body,"title",140);
            String artist=text(body,"artist",140);
            if(title.isBlank()){respond(exchange,400,"Missing title");return;}
            if(!provider.equals("Spotify") && !provider.equals("SoundCloud")
                && !provider.equals("YouTube Music")){
                respond(exchange,400,"Unsupported source");return;
            }
            long position=Math.min(86_400_000L,Math.max(0,
                body.has("positionMs")?body.get("positionMs").getAsLong():0L));
            boolean paused=body.has("paused")&&body.get("paused").getAsBoolean();
            Minecraft mc=Minecraft.getInstance();
            mc.execute(()->{
                VisionLyricsSource.receiveBrowserTrack(provider,artist,title,position,paused);
                maybeFetchLyrics(artist,title);
            });
            respond(exchange,200,"OK");
        }catch(Exception e){
            System.err.println("[VisionMod] Invalid local browser metadata: "+e);
            try{respond(exchange,400,"Bad request");}catch(Exception ignored){}
        }finally{exchange.close();}
    }
    private static String text(JsonObject body,String key,int limit){
        if(!body.has(key)||body.get(key).isJsonNull())return "";
        String s=body.get(key).getAsString().replaceAll("[\\p{Cntrl}]"," ").trim();
        return s.substring(0,Math.min(limit,s.length()));
    }
    private static void respond(HttpExchange e,int code,String body)throws java.io.IOException{
        byte[] b=body.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type","text/plain; charset=UTF-8");
        e.sendResponseHeaders(code,code==204?-1:b.length);
        if(code!=204)try(OutputStream o=e.getResponseBody()){o.write(b);}
    }

    private static void maybeFetchLyrics(String artist,String title) {
        if(artist.isBlank())return;
        String key=artist+"\n"+title;
        if(key.equals(lastSearched)||searchPending)return;
        long now=System.nanoTime();
        if(now-previousSearchAt<15_000_000_000L)return; // respect free API
        previousSearchAt=now;
        lastSearched=key;
        searchPending=true;
        String url="https://lrclib.net/api/get?artist_name="+
            URLEncoder.encode(artist,StandardCharsets.UTF_8)+"&track_name="+
            URLEncoder.encode(title,StandardCharsets.UTF_8);
        HttpRequest request=HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(6))
            .header("User-Agent","VisionMod/2.0.0 (https://github.com/De1po3oN/VisionMod-1.0.0)")
            .GET().build();
        HTTP.sendAsync(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .whenComplete((response,error)->{
                String lines="";
                if(error==null && response!=null && response.statusCode()==200 &&
                    response.body().length()<200_000){
                    try {
                        JsonObject lyrics=JsonParser.parseString(response.body()).getAsJsonObject();
                        if(lyrics.has("syncedLyrics") && !lyrics.get("syncedLyrics").isJsonNull())
                            lines=lyrics.get("syncedLyrics").getAsString();
                    }catch(Exception ignored){}
                }
                String found=lines;
                Minecraft.getInstance().execute(()->{
                    searchPending=false;
                    if(!key.equals(lastSearched))return;
                    VisionLyricsSource.setBrowserLyrics(artist,title,found);
                });
            });
    }

    public static boolean hasPairingToken(){return token!=null && token.matches("[0-9a-f]{32}");}
}
