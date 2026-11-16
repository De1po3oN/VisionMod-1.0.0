package com.visionmod;

import net.minecraft.client.Minecraft;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Timed lyrics only; exact lookup with a bounded search fallback, all state on the client thread. */
public final class VisionLyricsLookup {
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private static final URI SERVICE=URI.create("https://lrclib.net");
    private static final Map<String,String> CACHE=new LinkedHashMap<>();
    private static String appliedKey="",status="";
    private static boolean pending;
    private static long retryAt;
    private static int generation;
    private static CompletableFuture<String> inFlight;
    private VisionLyricsLookup(){}
    public static String status(){return status;}
    public static void trackChanged(){
        generation++;if(inFlight!=null)inFlight.cancel(true);
        inFlight=null;pending=false;appliedKey="";status="";retryAt=0;
    }
    public static void clear(){trackChanged();}
    public static void tick(){
        if(!VisionLyricsSource.mediaConnected())return;
        String artist=VisionLyricsSource.artist(),title=VisionLyricsSource.title();
        long duration=VisionLyricsSource.durationMs();
        String key=artist+"\n"+title+"\n"+duration/1000;
        if(CACHE.containsKey(key)){
            if(!key.equals(appliedKey))VisionLyricsSource.setMediaLyrics(artist,title,CACHE.get(key));
            appliedKey=key;
            status=CACHE.get(key).isBlank()?"Синхронизированного текста нет":
                VisionLyricsSource.positionKnown()?"Текст идёт по позиции песни":"Плеер не передаёт позицию песни";
            return;
        }
        long now=System.nanoTime();
        if(pending||now<retryAt)return;
        retryAt=now+15_000_000_000L;pending=true;status="Ищем синхронизированный текст…";
        final int current=generation;
        inFlight=fetch(SERVICE,LyricsSearch.query(artist,title),duration);
        inFlight.whenComplete((lyrics,error)->Minecraft.getInstance().execute(()->{
            if(current!=generation)return;
            pending=false;inFlight=null;
            if(error!=null){status="Нет связи с текстами · повторим автоматически";return;}
            if(CACHE.size()>=24)CACHE.remove(CACHE.keySet().iterator().next());
            CACHE.put(key,lyrics);
            if(artist.equals(VisionLyricsSource.artist())&&title.equals(VisionLyricsSource.title())){
                VisionLyricsSource.setMediaLyrics(artist,title,lyrics);appliedKey=key;
            }
        }));
    }
    static CompletableFuture<String> fetch(URI service,LyricsSearch.Query query,long duration){
        if(query.title().isBlank())return CompletableFuture.completedFuture("");
        String search=query.artist().isBlank()?"q="+encode(query.title()):
            "track_name="+encode(query.title())+"&artist_name="+encode(query.artist());
        if(query.artist().isBlank())return request(service.resolve("/api/search?"+search),query,duration);
        String exact="/api/get?track_name="+encode(query.title())+"&artist_name="+encode(query.artist())+
            (duration>0?"&duration="+duration/1000.0:"");
        CompletableFuture<String> result=new CompletableFuture<>();
        var active=new java.util.concurrent.atomic.AtomicReference<CompletableFuture<String>>();
        result.whenComplete((text,error)->{if(result.isCancelled()&&active.get()!=null)active.get().cancel(true);});
        CompletableFuture<String> first=request(service.resolve(exact),query,duration);active.set(first);
        first.whenComplete((text,error)->{
            if(result.isDone())return;
            if(error!=null){result.completeExceptionally(error);return;}
            if(!text.isBlank()){result.complete(text);return;}
            CompletableFuture<String> fallback=request(service.resolve("/api/search?"+search),query,duration);active.set(fallback);
            if(result.isCancelled())fallback.cancel(true);
            fallback.whenComplete((found,failure)->{if(failure==null)result.complete(found);else result.completeExceptionally(failure);});
        });
        return result;
    }
    private static CompletableFuture<String> request(URI uri,LyricsSearch.Query query,long duration){
        HttpRequest request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(6))
            .header("User-Agent","VisionMod/2.0.0 (https://github.com/De1po3oN/VisionMod-1.0.0)").GET().build();
        CompletableFuture<HttpResponse<byte[]>> response=HTTP.sendAsync(request,info->new BoundedLyricsBody());
        CompletableFuture<String> result=response.thenApply(reply->{
            if(reply.statusCode()==404)return "";
            if(reply.statusCode()!=200)throw new IllegalStateException("Lyrics HTTP "+reply.statusCode());
            return LyricsSearch.select(new String(reply.body(),StandardCharsets.UTF_8),query,duration);
        });
        result.whenComplete((value,error)->{if(result.isCancelled())response.cancel(true);});
        return result;
    }
    private static String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
}
