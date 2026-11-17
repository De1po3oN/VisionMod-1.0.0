package com.visionmod;

import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class MultiLyricsRegressionTest {
    private static final AtomicInteger checks=new AtomicInteger();
    private static void check(boolean value,String reason){checks.incrementAndGet();if(!value)throw new AssertionError(reason);}
    private static final String TOKEN="vision-fixture-token-0123456789";
    private static final String TIMED="[00:00.00]Actual first line\n[00:01.00]Actual next line";
    private static final String LRC="{\"trackName\":\"Song\",\"artistName\":\"Artist\",\"duration\":200,\"syncedLyrics\":\"[00:00]First\\n[00:01]Second\"}";
    private static final String PLAIN="{\"trackName\":\"Song\",\"artistName\":\"Artist\",\"duration\":200,\"plainLyrics\":\"Real words\\nMore real words\"}";
    private static void reply(HttpExchange exchange,int code,String body)throws java.io.IOException{
        byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(code,bytes.length);
        try(var out=exchange.getResponseBody()){out.write(bytes);}finally{exchange.close();}
    }
    public static void main(String[] args)throws Exception{
        check(YandexLyricsClient.sign("123",1700000000).equals("0Vud0sSibSPxZ9g889XHJBtHLa1dj/Cn39Hi8FCKB3g="),"Yandex signature matches an independent protocol vector");
        var query=LyricsSearch.query("Artist","Song — Яндекс Музыка");
        check(query.title().equals("Song"),"provider branding is removed from the lookup");
        check(LyricsSearch.matches("Ёлка","Artist",LyricsSearch.query("Artist","Елка")),"Russian ё/е metadata matches");
        check(LyricsSearch.matches("Song","Artist, Guest",LyricsSearch.query("Artist feat. Guest","Song")),"collaboration metadata does not hide the actual track");
        check(!LyricsSearch.matches("Song","Artist Tribute Band",query),"a tribute performer is not treated as the requested artist");
        var plain=LyricsSearch.selectResult(PLAIN,query,200000,"LRCLIB");
        check(plain.hasText()&&plain.timed().isBlank()&&!plain.plain().isBlank(),"plain words are retained without fabricated timestamps");
        check(LyricsSearch.select(PLAIN,query,200000).isEmpty(),"the legacy timed probe still requires timestamps");
        check(LyricsSearch.selectResult(PLAIN,query,240000,"LRCLIB")==LyricsResult.EMPTY,"wrong-duration plain result rejected too");
        check(new LyricsResult("", "word ".repeat(60),"test").plainLines().stream().allMatch(s->s.length()<=50),"plain lines fit the world plane without deleting the rest");
        check(new LyricsResult(TIMED,"","test").plainLines().get(0).equals("Actual first line"),"missing player position can expose words without timestamp tags");
        var yandexTrack=new SystemMediaTrack("org.mpris.MediaPlayer2.YandexMusic","Song","Artist",0,false);
        var browserYandex=SystemMediaTrack.fromMpris("org.mpris.MediaPlayer2.chromium","{'xesam:title': <'Song'>, 'xesam:url': <'https://music.yandex.ru/album/1/track/123'>, 'PlaybackStatus': <'Playing'>, 'Position': <int64 1000000>}");
        check(yandexTrack.isYandex()&&browserYandex.isYandex(),"Yandex app and browser MPRIS URL recognized");
        check(YandexLyricsClient.trackId(browserYandex.sourceUrl()).equals("123"),"browser track identity is used directly");
        check(YandexLyricsClient.trackId("https://music.yandex.ru@other.example/track/123").isEmpty(),"a deceptive URL cannot supply a Yandex track identity");
        check(!new SystemMediaTrack("YandexBrowser","Music video","Artist",0,false).isYandex(),"Yandex Browser alone does not identify the music service");
        check(yandexTrack.selectionScore()>new SystemMediaTrack("Spotify","Song","Artist",0,false).selectionScore(),"active Yandex is selected ahead of another active player");
        check(new SystemMediaTrack("Spotify","Song","Artist",0,false).selectionScore()>new SystemMediaTrack("YandexMusic","Song","Artist",0,true).selectionScore(),"paused Yandex cannot override a currently playing track");
        check(!YandexLyricsClient.downloadAllowed(URI.create("https://api.music.yandex.net"),URI.create("https://other.example/lyrics")),"signed content cannot point at an unrelated host");
        check(!YandexLyricsClient.downloadAllowed(URI.create("https://api.music.yandex.net"),URI.create("http://127.0.0.1/lyrics")),"production signed content cannot point at local HTTP");
        check(YandexLyricsClient.downloadAllowed(URI.create("https://api.music.yandex.net"),URI.create("https://storage.mds.yandex.net/lyrics")),"Yandex storage URL accepted");
        AtomicInteger mode=new AtomicInteger(),catalogHits=new AtomicInteger(),yandexHits=new AtomicInteger(),ovhHits=new AtomicInteger(),downloadAuthLeaks=new AtomicInteger();
        CountDownLatch slowStarted=new CountDownLatch(1),slowRelease=new CountDownLatch(1);
        var threads=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"lyrics-fixture");t.setDaemon(true);return t;});
        var server=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);server.setExecutor(threads);
        URI base=URI.create("http://127.0.0.1:"+server.getAddress().getPort());
        server.createContext("/api/",exchange->{
            catalogHits.incrementAndGet();int m=mode.get();
            if(m==6){slowStarted.countDown();try{slowRelease.await(2,TimeUnit.SECONDS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}try{reply(exchange,404,"");}catch(java.io.IOException expected){}return;}
            if(m==8){reply(exchange,200,exchange.getRequestURI().getPath().endsWith("/get")?PLAIN:"{broken");return;}
            if(m==0)reply(exchange,200,LRC);else if(m==1)reply(exchange,200,PLAIN);else if(m==2)reply(exchange,503,"");else reply(exchange,404,"");
        });
        server.createContext("/search",exchange->{
            yandexHits.incrementAndGet();check(("OAuth "+TOKEN).equals(exchange.getRequestHeaders().getFirst("Authorization")),"OAuth stays on the Yandex API request");
            if(mode.get()==4){reply(exchange,401,"{}");return;}
            if(mode.get()==7){reply(exchange,451,"{}");return;}
            String artist=mode.get()==5?"Unrelated Artist":"Artist";
            reply(exchange,200,"{\"result\":{\"tracks\":{\"results\":[{\"id\":123,\"title\":\"Song\",\"artists\":[{\"name\":\""+artist+"\"}],\"durationMs\":200000}]}}}");
        });
        server.createContext("/tracks/",exchange->{
            yandexHits.incrementAndGet();check(("OAuth "+TOKEN).equals(exchange.getRequestHeaders().getFirst("Authorization")),"lyric metadata uses the account token");
            check(exchange.getRequestURI().getRawQuery().contains("sign=")&&exchange.getRequestURI().getRawQuery().contains("timeStamp="),"authenticated lyrics request has a timestamp and signature");
            if(mode.get()==3&&exchange.getRequestURI().getQuery().contains("format=LRC")){reply(exchange,404,"{}");return;}
            reply(exchange,200,"{\"result\":{\"downloadUrl\":\""+base+"/content\"}}");
        });
        server.createContext("/content",exchange->{
            if(exchange.getRequestHeaders().getFirst("Authorization")!=null)downloadAuthLeaks.incrementAndGet();
            reply(exchange,200,mode.get()==3?"Words from Yandex\nSecond actual line":TIMED);
        });
        server.createContext("/v1/",exchange->{ovhHits.incrementAndGet();reply(exchange,200,"{\"lyrics\":\"Fallback words\\nNext fallback line\"}");});
        server.start();
        try{
            var found=VisionLyricsLookup.fetchAll(base,base,base,query,200000,"").get(5,TimeUnit.SECONDS);
            check(!found.timed().isBlank()&&found.source().equals("LRCLIB"),"timed catalog completes without a Yandex account");
            check(yandexHits.get()==0&&ovhHits.get()==0,"no credentialless private API requests or unnecessary plain lookup");
            mode.set(1);found=VisionLyricsLookup.fetchAll(base,base,base,query,200000,"").get(5,TimeUnit.SECONDS);
            check(found.timed().isBlank()&&found.plain().contains("Real words")&&ovhHits.get()==0,"matching plain catalog result retained after searching for timed variants");
            mode.set(8);found=VisionLyricsLookup.fetchAll(base,base,base,query,200000,"").get(5,TimeUnit.SECONDS);
            check(found.source().equals("LRCLIB")&&found.plain().contains("Real words"),"a later malformed search cannot discard real words already found");
            mode.set(2);found=VisionLyricsLookup.fetchAll(base,base,base,query,200000,TOKEN).get(5,TimeUnit.SECONDS);
            check(found.source().equals("Яндекс Музыка")&&found.timed().contains("Actual next"),"Yandex LRC survives a failure of the primary service");
            check(downloadAuthLeaks.get()==0,"OAuth is never forwarded to the signed content URL");
            int withoutSearch=yandexHits.get();found=VisionLyricsLookup.fetchAll(base,base,base,query,200000,TOKEN,"https://music.yandex.ru/album/1/track/123").get(5,TimeUnit.SECONDS);
            check(found.source().equals("Яндекс Музыка")&&yandexHits.get()==withoutSearch+1,"known browser track skips fuzzy catalog search");
            mode.set(3);found=VisionLyricsLookup.fetchAll(base,base,base,query,200000,TOKEN).get(5,TimeUnit.SECONDS);
            check(found.timed().isBlank()&&found.plain().contains("Words from Yandex"),"TEXT format is tried when Yandex has no LRC");
            YandexMusicAccess.connect(TOKEN);
            mode.set(4);found=VisionLyricsLookup.fetchAll(base,base,base,query,200000,TOKEN).get(5,TimeUnit.SECONDS);
            check(found.source().equals("lyrics.ovh")&&found.plain().contains("Fallback words"),"a denied Yandex token does not block other lyrics sources");
            check(YandexMusicAccess.status().contains("отклонил токен"),"rejected credentials are explained without showing the token");
            mode.set(7);found=VisionLyricsLookup.fetchAll(base,base,base,query,200000,TOKEN).get(5,TimeUnit.SECONDS);
            check(found.source().equals("lyrics.ovh")&&YandexMusicAccess.status().contains("региона"),"regional refusal is reported and other providers continue");
            mode.set(5);int apiBefore=yandexHits.get();found=VisionLyricsLookup.fetchAll(base,base,base,query,200000,TOKEN).get(5,TimeUnit.SECONDS);
            check(found.source().equals("lyrics.ovh")&&yandexHits.get()==apiBefore+1,"wrong artist is rejected before asking for their lyrics");
            mode.set(6);int requestsBefore=catalogHits.get(),fallbacksBefore=ovhHits.get();
            var pending=VisionLyricsLookup.fetchAll(base,base,base,query,200000,"");
            check(slowStarted.await(2,TimeUnit.SECONDS),"cancellation fixture received the HTTP request");pending.cancel(true);slowRelease.countDown();Thread.sleep(150);
            check(pending.isCancelled()&&catalogHits.get()==requestsBefore+1&&ovhHits.get()==fallbacksBefore,"cancellation stops subsequent provider requests");
            check(!YandexMusicAccess.connect("password"),"short password is not accepted as OAuth");
            check(YandexMusicAccess.connect(TOKEN)&&YandexMusicAccess.connected(),"a user token can be held for this process");
            YandexMusicAccess.disconnect();check(!YandexMusicAccess.connected(),"disconnect clears the in-memory token");
        }finally{slowRelease.countDown();server.stop(0);threads.shutdownNow();}
        System.out.println("PASS: "+checks+" multi-provider/Yandex assertions (local HTTP, real request signing, plaintext, cancellation and credential isolation)");
    }
}
