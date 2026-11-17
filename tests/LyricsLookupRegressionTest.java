package com.visionmod;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class LyricsLookupRegressionTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        String timed="[00:00.00]Start\n[00:01.00]Next";
        String correct="{\"trackName\":\"Song\",\"artistName\":\"Artist\",\"duration\":200,\"syncedLyrics\":\"[00:00]Start\\n[00:01]Next\"}";
        var query=LyricsSearch.query("","Artist - Song (Official Video)");
        check(query.artist().equals("Artist")&&query.title().equals("Song"),"browser title normalized");
        for(String separator:new String[]{" — "," – "," | "," - "}){
            var sparse=LyricsSearch.query("","Artist"+separator+"Song [Official Music Video 4K]");
            check(sparse.artist().equals("Artist")&&sparse.title().equals("Song"),"sparse browser separator "+separator);
        }
        check(LyricsSearch.query("Artist - Topic","Song").artist().equals("Artist"),"YouTube Topic channel normalized");
        check(LyricsSearch.query("ArtistVEVO","Artist — Song").title().equals("Song"),"duplicated artist prefix removed");
        check(LyricsSearch.query("Artist","Song — Яндекс Музыка").title().equals("Song"),"Yandex presentation suffix removed");
        check(LyricsSearch.matches("Song","Artist",LyricsSearch.query("Artist","Song (Текст песни)")),"Russian lyric presentation label normalized");
        check(!LyricsSearch.matches("Song (Live)","Artist",query),"live version remains distinct");
        check(!LyricsSearch.matches("Song (Remix)","Artist",query),"remix version remains distinct");
        check(LyricsSearch.select(correct,query,200000).contains("Next"),"exact timed result selected");
        check(LyricsSearch.select(correct,query,240000).isEmpty(),"wrong song version rejected by duration");
        check(LyricsSearch.select(correct,LyricsSearch.query("Other","Song"),200000).isEmpty(),"wrong performer rejected");
        check(LyricsSearch.select(correct,LyricsSearch.query("","Song"),200000).contains("Next"),"missing artist does not prevent matching");
        check(LyricsSearch.select(correct.replace("\"syncedLyrics\":\"[00:00]Start\\n[00:01]Next\"","\"plainLyrics\":\"Untimed\""),query,200000).isEmpty(),"plain text cannot pretend to be synchronized");
        AtomicInteger gets=new AtomicInteger(),searches=new AtomicInteger();
        HttpServer server=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);
        server.createContext("/api/get",exchange->{
            gets.incrementAndGet();exchange.sendResponseHeaders(404,-1);exchange.close();
        });
        server.createContext("/api/search",exchange->{
            searches.incrementAndGet();byte[] body=("["+correct+"]").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });
        server.start();
        try{
            URI endpoint=URI.create("http://127.0.0.1:"+server.getAddress().getPort());
            String found=VisionLyricsLookup.fetch(endpoint,query,200000).get(4,TimeUnit.SECONDS);
            check(found.contains("Next")&&gets.get()==1&&searches.get()==1,"real HTTP 404 falls back to timed search");
            found=VisionLyricsLookup.fetch(endpoint,LyricsSearch.query("","Song"),200000).get(4,TimeUnit.SECONDS);
            check(found.contains("Next")&&gets.get()==1&&searches.get()==2,"real HTTP search works without artist");
        }finally{server.stop(0);}
        System.out.println("PASS: "+checks+" lyric lookup assertions (local HTTP fixture)");
    }
}
