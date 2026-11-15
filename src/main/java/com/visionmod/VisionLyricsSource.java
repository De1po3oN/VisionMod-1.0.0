package com.visionmod;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional local timed lyric source. A media player or bridge may update the
 * user's own lyrics.lrc file; we never guess copyrighted lyrics or pretend to
 * read another app's audio stream.
 *
 * Accepted format: [00:10.50]First line, [00:14.00]Second line.
 * File updates restart the local timeline (no undocumented player sync).
 */
public final class VisionLyricsSource {
    private static final Path FILE=FabricLoader.getInstance().getConfigDir()
        .resolve("visionmod/lyrics.lrc");
    private static final Pattern TIMESTAMP=Pattern.compile(
        "^\\[(\\d{1,3}):(\\d{2})(?:\\.(\\d{1,3}))?\\]\\s*(.*)$");
    private record Line(long millis,String text) {}
    private static List<Line> timeline=List.of();
    private static long lastPollNanos;
    private static long loadedStamp=Long.MIN_VALUE;
    private static long startedAt=System.nanoTime();
    private static String currentPath="No lyric file";
    // Track position supplied explicitly by the paired browser extension.
    // No microphone permission or inaccessible browser application scraping.
    private static List<Line> browserLines=List.of();
    private static String browserArtist="",browserTitle="",browserProvider="";
    private static long browserPositionMs,browserReceivedNanos;
    private static boolean browserPaused;
    private VisionLyricsSource(){}

    public static String fileLocation(){return FILE.toString();}
    public static boolean browserConnected(){
        return !browserTitle.isBlank() && System.nanoTime()-browserReceivedNanos<12_000_000_000L;
    }
    public static boolean active(){return browserConnected() || !timeline.isEmpty();}
    public static String status(){
        if(browserConnected())
            return browserProvider+": "+browserArtist+" - "+browserTitle+
                (browserLines.isEmpty()?" (lyrics pending/unavailable)":" (synced)");
        return !timeline.isEmpty()?"Local timed lyrics loaded":"Manual text / no linked player";
    }
    public static void receiveBrowserTrack(String provider,String artist,String title,
                                           long positionMs,boolean paused){
        boolean newTrack=!artist.equals(browserArtist)||!title.equals(browserTitle)
            ||!provider.equals(browserProvider);
        browserArtist=artist;browserTitle=title;browserProvider=provider;
        browserPositionMs=positionMs;browserPaused=paused;
        browserReceivedNanos=System.nanoTime();
        if(newTrack)browserLines=List.of();
    }
    public static void setBrowserLyrics(String artist,String title,String syncedLrc){
        if(!artist.equals(browserArtist)||!title.equals(browserTitle))return;
        browserLines=parse(List.of((syncedLrc==null?"":syncedLrc).split("\\R")));
    }
    public static void clearBrowserTrack(){
        browserArtist="";browserTitle="";browserProvider="";
        browserLines=List.of();browserReceivedNanos=0;
    }
    private static long livePosition(){
        long elapsed=browserPaused?0:Math.max(0,(System.nanoTime()-browserReceivedNanos)/1_000_000L);
        return Math.max(0,browserPositionMs+elapsed);
    }
    private static String lineAt(List<Line> entries,long ms) {
        if(entries.isEmpty())return null;
        int low=0,high=entries.size()-1,found=-1;
        while(low<=high){
            int middle=(low+high)>>>1;
            if(entries.get(middle).millis()<=ms){found=middle;low=middle+1;}
            else high=middle-1;
        }
        return found<0 ? "♪" : entries.get(found).text();
    }

    public static void poll(){
        long now=System.nanoTime();
        if(now-lastPollNanos<1_000_000_000L)return;
        lastPollNanos=now;
        try {
            long stamp=Files.isRegularFile(FILE)?Files.getLastModifiedTime(FILE).toMillis():-1L;
            if(stamp==loadedStamp)return;
            loadedStamp=stamp;
            timeline=List.of();
            if(stamp<0){currentPath="No lyric file";return;}
            // Bound IO work; never read arbitrary huge files in render callbacks.
            if(Files.size(FILE)>128*1024){currentPath="Lyrics file too large";return;}
            timeline=parse(Files.readAllLines(FILE,StandardCharsets.UTF_8));
            startedAt=now;
            currentPath=timeline.isEmpty()?"No timed lines":"Timed lyrics loaded";
        } catch(IOException e) {
            timeline=List.of();
            currentPath="Lyrics read failed";
            System.err.println("[VisionMod] LRC: "+e);
        }
    }
    private static List<Line> parse(List<String> input){
        List<Line> result=new ArrayList<>();
        for(String row:input){
            Matcher m=TIMESTAMP.matcher(row.trim());
            if(!m.matches())continue;
            int mm=Integer.parseInt(m.group(1)),ss=Integer.parseInt(m.group(2));
            if(ss>59)continue;
            String frac=m.group(3);
            int ms=0;
            if(frac!=null) ms=frac.length()==1?Integer.parseInt(frac)*100:
                frac.length()==2?Integer.parseInt(frac)*10:Integer.parseInt(frac);
            String lyric=m.group(4).trim();
            if(lyric.isBlank())continue;
            if(lyric.length()>140)lyric=lyric.substring(0,140);
            result.add(new Line((mm*60L+ss)*1000+ms,lyric));
            if(result.size()>=1200)break;
        }
        result.sort(Comparator.comparingLong(Line::millis));
        return List.copyOf(result);
    }
    public static String current(){
        if(browserConnected()){
            String line=lineAt(browserLines,livePosition());
            // Never leave an enabled overlay blank when a song is playing
            // but no synced lyric record is available for this track.
            return line!=null ? line : "♪ "+browserTitle+" — "+browserArtist;
        }
        poll();
        if(timeline.isEmpty())return null;
        return lineAt(timeline,Math.max(0,(System.nanoTime()-startedAt)/1_000_000L));
    }
    public static String testLine(String... rows){
        List<Line> parsed=parse(List.of(rows));
        return parsed.size()<2 ? "" : parsed.get(1).text();
    }
}
