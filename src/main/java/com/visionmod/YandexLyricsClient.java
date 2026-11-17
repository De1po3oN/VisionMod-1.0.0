package com.visionmod;

import com.google.gson.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Independent client for the authenticated track-lyrics API. No browser cookies or passwords. */
final class YandexLyricsClient {
    // Public client-protocol compatibility value, not a user's OAuth credential.
    private static final String SIGNING_KEY="p93jhgh689SBReK6ghtw62";
    static CompletableFuture<LyricsResult> fetch(URI service,LyricsSearch.Query query,long duration,String token,LyricsHttp.Flight flight){
        return fetch(service,query,duration,token,flight,"");
    }
    static CompletableFuture<LyricsResult> fetch(URI service,LyricsSearch.Query query,long duration,String token,LyricsHttp.Flight flight,String sourceUrl){
        if(token.isBlank())return CompletableFuture.completedFuture(LyricsResult.EMPTY);
        String known=trackId(sourceUrl);
        if(!known.isBlank())return bothFormats(service,known,token,flight);
        URI search=service.resolve("/search?type=track&page=0&nocorrect=true&text="+LyricsHttp.encode(query.artist()+" "+query.title()));
        return LyricsHttp.get(search,token,flight).thenCompose(reply->{
            if(!allowed(reply.code(),token))return CompletableFuture.completedFuture(LyricsResult.EMPTY);
            String id=selectTrack(reply.text(),query,duration);
            if(id.isEmpty())return CompletableFuture.completedFuture(LyricsResult.EMPTY);
            return bothFormats(service,id,token,flight);
        });
    }
    private static CompletableFuture<LyricsResult> bothFormats(URI service,String id,String token,LyricsHttp.Flight flight){
        return words(service,id,"LRC",token,flight).thenCompose(result->result.hasText()?CompletableFuture.completedFuture(result):words(service,id,"TEXT",token,flight));
    }
    static String trackId(String value){
        try{
            URI uri=URI.create(value);String host=uri.getHost();
            if(host==null||uri.getUserInfo()!=null||!host.toLowerCase(java.util.Locale.ROOT).matches("music\\.yandex\\.(?:ru|com|kz|by|uz)"))return "";
            var match=java.util.regex.Pattern.compile("/(?:track|tracks)/([0-9]{1,20})(?:/|$)").matcher(uri.getPath());
            return match.find()?match.group(1):"";
        }catch(IllegalArgumentException ignored){return "";}
    }
    static String selectTrack(String json,LyricsSearch.Query query,long duration){
        JsonObject root=JsonParser.parseString(json).getAsJsonObject();
        if(!root.has("result")||!root.get("result").isJsonObject())return "";
        var result=root.getAsJsonObject("result");if(!result.has("tracks")||!result.get("tracks").isJsonObject())return "";
        var tracks=result.getAsJsonObject("tracks");if(!tracks.has("results")||!tracks.get("results").isJsonArray())return "";
        var items=tracks.getAsJsonArray("results");String best="";double score=Double.POSITIVE_INFINITY;
        for(int i=0;i<Math.min(items.size(),40);i++){
            if(!items.get(i).isJsonObject())continue;var item=items.get(i).getAsJsonObject();
            String title=text(item,"title"),version=text(item,"version");if(!version.isBlank())title+=" ("+version+")";
            StringBuilder artist=new StringBuilder();
            if(item.has("artists")&&item.get("artists").isJsonArray())for(var value:item.getAsJsonArray("artists"))if(value.isJsonObject())artist.append(text(value.getAsJsonObject(),"name")).append(", ");
            if(!LyricsSearch.matches(title,artist.toString(),query))continue;
            double difference=0;
            if(duration>0){try{difference=Math.abs(item.get("durationMs").getAsDouble()-duration);}catch(RuntimeException ignored){continue;}if(!Double.isFinite(difference)||difference>5000)continue;}
            String id=text(item,"id");if(!id.matches("[0-9]{1,20}"))continue;
            if(difference<score){score=difference;best=id;}
        }
        return best;
    }
    private static CompletableFuture<LyricsResult> words(URI service,String id,String format,String token,LyricsHttp.Flight flight){
        long timestamp=Instant.now().getEpochSecond();
        URI uri=service.resolve("/tracks/"+id+"/lyrics?format="+format+"&timeStamp="+timestamp+"&sign="+LyricsHttp.encode(sign(id,timestamp)));
        return LyricsHttp.get(uri,token,flight).thenCompose(reply->{
            if(!allowed(reply.code(),token))return CompletableFuture.completedFuture(LyricsResult.EMPTY);
            var root=JsonParser.parseString(reply.text());
            if(!root.isJsonObject()||!root.getAsJsonObject().has("result")||!root.getAsJsonObject().get("result").isJsonObject())return CompletableFuture.completedFuture(LyricsResult.EMPTY);
            String value=text(root.getAsJsonObject().getAsJsonObject("result"),"downloadUrl");
            URI download;
            try{download=URI.create(value);}catch(IllegalArgumentException ignored){return CompletableFuture.completedFuture(LyricsResult.EMPTY);}
            if(!downloadAllowed(service,download))return CompletableFuture.completedFuture(LyricsResult.EMPTY);
            // Signed storage URL is fetched without forwarding the account credential.
            return LyricsHttp.get(download,"",flight).thenApply(body->{
                if(body.code()!=200||body.text().stripLeading().startsWith("<")||body.text().stripLeading().startsWith("{"))return LyricsResult.EMPTY;
                var lyrics=new LyricsResult(format.equals("LRC")?body.text():"",format.equals("LRC")?"":body.text(),"Яндекс Музыка");
                if(lyrics.hasText())YandexMusicAccess.report(token,"Яндекс Музыка · доступ к текстам работает");
                return lyrics;
            });
        });
    }
    static boolean downloadAllowed(URI service,URI download){
        String host=download.getHost();if(host==null||download.getUserInfo()!=null)return false;
        // Exact-origin HTTP is only useful to an injected loopback fixture.
        if(service.getScheme().equals("http")&&host.equals(service.getHost())&&download.getPort()==service.getPort()&&download.getScheme().equals("http"))return true;
        return download.getScheme().equals("https")&&(host.equals("yandex.net")||host.endsWith(".yandex.net")||host.equals("yandex.ru")||host.endsWith(".yandex.ru"));
    }
    private static boolean allowed(int code,String token){
        if(code==401)YandexMusicAccess.report(token,"Яндекс отклонил токен · подключи действующий OAuth-токен");
        else if(code==403)YandexMusicAccess.report(token,"Яндекс не разрешил доступ к этому тексту");
        else if(code==429)YandexMusicAccess.report(token,"Яндекс ограничил запросы · повторим позже");
        else if(code==451)YandexMusicAccess.report(token,"Яндекс недоступен из текущего региона");
        else if(code>=500)YandexMusicAccess.report(token,"Яндекс временно недоступен · попробуем другие источники");
        return code==200;
    }
    static String sign(String id,long timestamp){
        try{
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(SIGNING_KEY.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal((id+timestamp).getBytes(StandardCharsets.UTF_8)));
        }catch(java.security.GeneralSecurityException error){throw new IllegalStateException("HMAC-SHA256 unavailable",error);}
    }
    private static String text(JsonObject object,String name){return object.has(name)&&object.get(name).isJsonPrimitive()?object.get(name).getAsString():"";}
}
