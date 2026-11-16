package com.visionmod;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Optional asynchronous LRCLIB lookup; all mutable state stays on the client thread. */
public final class VisionLyricsLookup {
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private static final Map<String,String> CACHE=new LinkedHashMap<>();
    private static String appliedKey="",status="";
    private static boolean pending;
    private static long previousSearch;
    private static int generation;
    private static java.util.concurrent.CompletableFuture<HttpResponse<byte[]>> inFlight;
    private VisionLyricsLookup(){}
    public static String status(){return status;}
    public static void trackChanged(){appliedKey="";status="";}
    public static void clear(){generation++;if(inFlight!=null)inFlight.cancel(true);pending=false;trackChanged();}
    public static void tick(){
        if(!VisionLyricsSource.mediaConnected())return;
        String artist=VisionLyricsSource.artist(),title=VisionLyricsSource.title();
        if(artist.isBlank()){status="Плеер не передал исполнителя";return;}
        String key=artist+"\n"+title;
        if(CACHE.containsKey(key)){
            if(!key.equals(appliedKey))VisionLyricsSource.setMediaLyrics(artist,title,CACHE.get(key));
            appliedKey=key;
            status=CACHE.get(key).isBlank()?"Текста нет · показываем название трека":"Текст синхронизирован";
            return;
        }
        long now=System.nanoTime();
        if(pending || now-previousSearch<15_000_000_000L)return;
        previousSearch=now;pending=true;status="Ищем текст…";
        final int current=generation;
        String url="https://lrclib.net/api/get?artist_name="+URLEncoder.encode(artist,StandardCharsets.UTF_8)+
            "&track_name="+URLEncoder.encode(title,StandardCharsets.UTF_8);
        HttpRequest request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6))
            .header("User-Agent","VisionMod/2.0.0 (https://github.com/De1po3oN/VisionMod-1.0.0)").GET().build();
        inFlight=HTTP.sendAsync(request,info->new BoundedLyricsBody());
        inFlight.thenApply(response->{
            try{
                if(response.statusCode()==404)return new Result(true,"");
                if(response.statusCode()!=200)return new Result(false,"");
                var doc=JsonParser.parseString(new String(response.body(),StandardCharsets.UTF_8)).getAsJsonObject();
                String lyrics=doc.has("syncedLyrics")&&!doc.get("syncedLyrics").isJsonNull()?doc.get("syncedLyrics").getAsString():"";
                if(LrcTimeline.parse(lyrics).isEmpty())lyrics="";
                return new Result(true,lyrics);
            }catch(Exception e){return new Result(false,"");}
        }).whenComplete((result,error)->Minecraft.getInstance().execute(()->{
            if(current!=generation)return;
            pending=false;
            if(error!=null||result==null||!result.success){
                status="Нет связи с текстами · повторим автоматически";return;
            }
            if(CACHE.size()>=24)CACHE.remove(CACHE.keySet().iterator().next());
            CACHE.put(key,result.lyrics);
            // If the song changed during the request, its cached result is kept for later.
            if(artist.equals(VisionLyricsSource.artist())&&title.equals(VisionLyricsSource.title())){
                VisionLyricsSource.setMediaLyrics(artist,title,result.lyrics);appliedKey=key;
            }
        }));
    }
    private record Result(boolean success,String lyrics){}
}
