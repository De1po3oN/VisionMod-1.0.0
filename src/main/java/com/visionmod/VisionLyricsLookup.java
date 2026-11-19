package com.visionmod;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Multiple bounded providers; real timestamps are preferred over plain words. */
public final class VisionLyricsLookup {
    private static final URI SERVICE=URI.create("https://lrclib.net"),YANDEX=URI.create("https://api.music.yandex.net"),PLAIN=URI.create("https://api.lyrics.ovh");
    private static final Map<String,LyricsResult> CACHE=new LinkedHashMap<>();
    private static String appliedKey="",status="";
    private static boolean pending;
    private static long retryAt;
    private static int generation;
    private static CompletableFuture<LyricsResult> inFlight;
    private VisionLyricsLookup(){}
    public static String status(){return status;}
    public static void trackChanged(){
        generation++;if(inFlight!=null)inFlight.cancel(true);
        inFlight=null;pending=false;appliedKey="";status="";retryAt=0;
    }
    public static void clear(){trackChanged();}
    public static void clearCache(){CACHE.clear();trackChanged();}
    public static void tick(){
        if(!VisionLyricsSource.mediaConnected())return;
        String artist=VisionLyricsSource.artist(),title=VisionLyricsSource.title();
        long duration=VisionLyricsSource.durationMs();String key=artist+"\n"+title+"\n"+duration/1000;
        String knownId=YandexLyricsClient.trackId(VisionLyricsSource.sourceUrl());if(!knownId.isBlank())key+="\n"+knownId;
        final String cacheKey=key;
        if(CACHE.containsKey(key)){
            if(!key.equals(appliedKey))VisionLyricsSource.setMediaLyrics(artist,title,CACHE.get(key));
            appliedKey=key;status=VisionLyricsSource.status();return;
        }
        long now=System.nanoTime();if(pending||now<retryAt)return;
        retryAt=now+15_000_000_000L;pending=true;status="Ищем слова · LRCLIB / Яндекс / запасной источник…";
        final int current=generation;
        inFlight=fetchAll(SERVICE,YANDEX,PLAIN,LyricsSearch.query(artist,title),duration,YandexMusicAccess.token(),VisionLyricsSource.sourceUrl());
        inFlight.whenComplete((lyrics,error)->Minecraft.getInstance().execute(()->{
            if(current!=generation)return;
            pending=false;inFlight=null;
            if(error!=null){status="Источники текстов недоступны · повторим автоматически";return;}
            if(!lyrics.hasText()){
                status=YandexMusicAccess.connected()?"Слова не найдены в доступных источниках":"Слова не найдены · можно подключить тексты Яндекса";
                retryAt=System.nanoTime()+60_000_000_000L;return;
            }
            if(CACHE.size()>=24)CACHE.remove(CACHE.keySet().iterator().next());CACHE.put(cacheKey,lyrics);
            if(artist.equals(VisionLyricsSource.artist())&&title.equals(VisionLyricsSource.title())){
                VisionLyricsSource.setMediaLyrics(artist,title,lyrics);appliedKey=cacheKey;status=VisionLyricsSource.status();
            }
        }));
    }
    /** Compatibility probe for the timed public catalog, without credentials. */
    public static CompletableFuture<String> fetch(URI service,LyricsSearch.Query query,long duration){
        LyricsHttp.Flight flight=new LyricsHttp.Flight();var result=new CompletableFuture<String>();
        result.whenComplete((value,error)->flight.cancel());
        catalog(catalogRequests(service,query,duration),duration,flight,0,LyricsResult.EMPTY,null)
            .whenComplete((lyrics,error)->{if(error!=null)result.completeExceptionally(error);else result.complete(lyrics.timed());});
        return result;
    }
    static CompletableFuture<LyricsResult> fetchAll(URI catalog,URI yandex,URI plain,LyricsSearch.Query query,long duration,String token){
        return fetchAll(catalog,yandex,plain,query,duration,token,"");
    }
    static CompletableFuture<LyricsResult> fetchAll(URI catalog,URI yandex,URI plain,LyricsSearch.Query query,long duration,String token,String sourceUrl){
        if(query.title().isBlank())return CompletableFuture.completedFuture(LyricsResult.EMPTY);
        LyricsHttp.Flight flight=new LyricsHttp.Flight();var result=new CompletableFuture<LyricsResult>();
        result.whenComplete((value,error)->flight.cancel());
        var best=new AtomicReference<>(LyricsResult.EMPTY);var remaining=new AtomicInteger(2);var failed=new AtomicBoolean();
        java.util.function.BiConsumer<LyricsResult,Throwable> accept=(lyrics,error)->{
            if(result.isDone())return;
            if(error!=null)failed.set(true);
            if(lyrics!=null&&lyrics.hasText()){
                if(!lyrics.timed().isBlank()){result.complete(lyrics);return;}
                best.compareAndSet(LyricsResult.EMPTY,lyrics);
            }
            if(remaining.decrementAndGet()!=0||result.isDone())return;
            if(best.get().hasText()){result.complete(best.get());return;}
            if(query.artist().isBlank()){if(failed.get())result.completeExceptionally(new java.io.IOException("Lyrics providers unavailable"));else result.complete(LyricsResult.EMPTY);return;}
            URI uri=plain.resolve("/v1/"+LyricsHttp.encode(query.artist()).replace("+","%20")+"/"+LyricsHttp.encode(query.title()).replace("+","%20"));
            LyricsHttp.get(uri,"",flight).whenComplete((reply,failure)->{
                if(result.isDone())return;
                if(failure!=null){result.completeExceptionally(failure);return;}
                if(reply.code()==404){if(failed.get())result.completeExceptionally(new java.io.IOException("Lyrics providers unavailable"));else result.complete(LyricsResult.EMPTY);return;}
                if(reply.code()!=200){result.completeExceptionally(new java.io.IOException("Plain lyrics HTTP "+reply.code()));return;}
                try{
                    var doc=JsonParser.parseString(reply.text()).getAsJsonObject();
                    result.complete(new LyricsResult("",doc.has("lyrics")?doc.get("lyrics").getAsString():"","lyrics.ovh"));
                }catch(RuntimeException problem){result.completeExceptionally(problem);}
            });
        };
        catalog(catalogRequests(catalog,query,duration),duration,flight,0,LyricsResult.EMPTY,null).whenComplete(accept);
        YandexLyricsClient.fetch(yandex,query,duration,token,flight,sourceUrl).whenComplete(accept);
        return result;
    }
    private record CatalogRequest(URI uri, LyricsSearch.Query validation) {}
    private record Attempt(LyricsResult lyrics, Throwable failure, boolean stop) {}

    private static List<CatalogRequest> catalogRequests(URI service, LyricsSearch.Query query, long duration) {
        var requests = new LinkedHashSet<CatalogRequest>();
        String artist = LyricsHttp.encode(query.artist());
        String title = LyricsHttp.encode(query.title());
        String fields = "track_name=" + title + "&artist_name=";
        if (!artist.isBlank()) {
            String length = duration > 0 ? "&duration=" + duration / 1000.0 : "";
            requests.add(new CatalogRequest(service.resolve("/api/get?" + fields + artist + length), query));
            requests.add(new CatalogRequest(service.resolve("/api/search?" + fields + artist), query));
            String primary = LyricsSearch.primaryArtistName(query.artist());
            if (!primary.isBlank() && !primary.equals(query.artist())) {
                requests.add(new CatalogRequest(service.resolve("/api/search?" + fields + LyricsHttp.encode(primary)), query));
            }
            requests.add(new CatalogRequest(service.resolve("/api/search?q=" + LyricsHttp.encode(query.artist() + " " + query.title())), query));
        }
        requests.add(new CatalogRequest(service.resolve("/api/search?q=" + title), query));
        if (!query.unsplitTitle().isBlank()) {
            // A dash might be part of the real title rather than an artist separator.
            var unsplit = new LyricsSearch.Query("", query.unsplitTitle());
            requests.add(new CatalogRequest(service.resolve("/api/search?q=" + LyricsHttp.encode(unsplit.title())), unsplit));
        }
        return List.copyOf(requests);
    }

    private static CompletableFuture<LyricsResult> catalog(List<CatalogRequest> requests, long duration,
            LyricsHttp.Flight flight, int index, LyricsResult best, Throwable failure) {
        if (flight.cancelled()) return CompletableFuture.failedFuture(new CancellationException());
        if (index >= requests.size() || flight.remainingNanos() <= 0) return finish(best, failure);

        CatalogRequest request = requests.get(index);
        return LyricsHttp.get(request.uri(), "", flight).handle((reply, error) -> {
            if (error != null) return new Attempt(LyricsResult.EMPTY, error, false);
            if (reply.code() == 404) return new Attempt(LyricsResult.EMPTY, null, false);
            if (reply.code() != 200) {
                boolean denied = reply.code() == 401 || reply.code() == 403 || reply.code() == 429 || reply.code() == 451;
                return new Attempt(LyricsResult.EMPTY, new IOException("Lyrics HTTP " + reply.code()), denied);
            }
            try {
                return new Attempt(LyricsSearch.selectResult(reply.text(), request.validation(), duration, "LRCLIB"), null, false);
            } catch (RuntimeException malformedReply) {
                return new Attempt(LyricsResult.EMPTY, malformedReply, false);
            }
        }).thenCompose(attempt -> {
            if (flight.cancelled()) return CompletableFuture.failedFuture(new CancellationException());
            if (!attempt.lyrics().timed().isBlank()) return CompletableFuture.completedFuture(attempt.lyrics());
            LyricsResult next = best.hasText() ? best : attempt.lyrics();
            Throwable nextFailure = attempt.failure() == null ? failure : attempt.failure();
            if (attempt.stop()) return finish(next, nextFailure);
            return catalog(requests, duration, flight, index + 1, next, nextFailure);
        });
    }

    private static CompletableFuture<LyricsResult> finish(LyricsResult best, Throwable failure) {
        return best.hasText() || failure == null ? CompletableFuture.completedFuture(best) : CompletableFuture.failedFuture(failure);
    }
}
