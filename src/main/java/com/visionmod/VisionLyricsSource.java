package com.visionmod;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** System media playback is authoritative; local LRC is an optional offline fallback. */
public final class VisionLyricsSource {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("visionmod/lyrics.lrc");
    private static LrcTimeline timeline = LrcTimeline.EMPTY, mediaLines = LrcTimeline.EMPTY;
    private static long lastPollNanos, loadedStamp = Long.MIN_VALUE, startedAt = System.nanoTime();
    private static String mediaArtist = "", mediaTitle = "", mediaProvider = "";
    private static long mediaPositionMs, mediaReceivedNanos;
    private static long mediaDurationMs;
    private static double mediaRate=1;
    private static boolean mediaPaused,mediaPositionKnown=true;
    private static java.util.List<String> plainLines=java.util.List.of();
    private static int plainIndex;
    private static String lyricProvider="";
    private static String mediaUrl="";
    private VisionLyricsSource() {}

    public static String fileLocation() { return FILE.toString(); }
    // Stop presenting a stale song after a native reader disconnects.
    public static boolean mediaConnected() {
        return !mediaTitle.isBlank() && System.nanoTime() - mediaReceivedNanos < 10_000_000_000L;
    }
    public static boolean active() { return mediaConnected() || !timeline.isEmpty(); }
    public static String status() {
        if (mediaConnected()) {
            if(plainMode())return (mediaPositionKnown?"Без таймкодов":"Плеер не передаёт позицию")+" · строка "+(plainIndex+1)+"/"+plainLines.size()+" · "+lyricProvider;
            return !mediaPositionKnown?"Плеер не передаёт позицию песни":mediaLines.isEmpty()&&timeline.isEmpty()?"Трек подключён · ищем слова":
                (mediaPaused?"Пауза":"Строки синхронизированы")+(mediaLines.isEmpty()&&!timeline.isEmpty()?" · локальный LRC":lyricProvider.isEmpty()?"":" · "+lyricProvider);
        }
        return !timeline.isEmpty() ? "Локальный LRC" : "Включи музыку в браузере";
    }
    public static String trackLabel() { return mediaTitle + (mediaArtist.isBlank() ? "" : " — " + mediaArtist); }
    public static String artist() { return mediaArtist; }
    public static String title() { return mediaTitle; }
    public static long durationMs(){return mediaDurationMs;}
    public static String sourceUrl(){return mediaUrl;}
    public static boolean positionKnown(){return mediaPositionKnown;}
    public static void receiveMediaTrack(String provider, String artist, String title, long positionMs, boolean paused) {
        receiveMediaTrack(new SystemMediaTrack(provider,title,artist,positionMs,paused),System.nanoTime());
    }
    public static void receiveMediaTrack(SystemMediaTrack track,long observedNanos){
        String oldId=YandexLyricsClient.trackId(mediaUrl),newId=YandexLyricsClient.trackId(track.sourceUrl());
        boolean urlChanged=!newId.isEmpty()&&!oldId.isEmpty()?!newId.equals(oldId):!track.sourceUrl().equals(mediaUrl);
        boolean changed=!track.artist().equals(mediaArtist)||!track.title().equals(mediaTitle)||!track.provider().equals(mediaProvider)||urlChanged;
        mediaArtist=track.artist();mediaTitle=track.title();mediaProvider=track.provider();mediaUrl=track.sourceUrl();
        mediaPositionMs=track.positionMs();mediaPaused=track.paused();mediaDurationMs=track.durationMs();
        mediaRate=track.playbackRate();mediaPositionKnown=track.positionKnown();
        mediaReceivedNanos=Math.min(System.nanoTime(),observedNanos);
        if (changed) { mediaLines = LrcTimeline.EMPTY;plainLines=java.util.List.of();plainIndex=0;lyricProvider="";VisionLyricsLookup.trackChanged(); }
    }
    public static void setMediaLyrics(String artist, String title, String lrc) {
        setMediaLyrics(artist,title,new LyricsResult(lrc,"","LRC"));
    }
    public static void setMediaLyrics(String artist,String title,LyricsResult lyrics){
        if(artist.equals(mediaArtist)&&title.equals(mediaTitle)){
            mediaLines=LrcTimeline.parse(lyrics.timed());plainLines=lyrics.plainLines();plainIndex=0;lyricProvider=lyrics.source();
        }
    }
    public static boolean plainMode(){return mediaConnected()&&(!mediaPositionKnown||(mediaLines.isEmpty()&&timeline.isEmpty()))&&!plainLines.isEmpty();}
    public static int plainLineCount(){return plainLines.size();}
    public static int plainLineIndex(){return plainIndex;}
    public static void movePlainLine(int direction){if(!plainLines.isEmpty())plainIndex=Math.floorMod(plainIndex+direction,plainLines.size());}
    public static void clearMediaTrack() {
        mediaArtist = mediaTitle = mediaProvider = mediaUrl = "";
        mediaLines = LrcTimeline.EMPTY;plainLines=java.util.List.of();plainIndex=0;lyricProvider="";mediaReceivedNanos = 0; VisionLyricsLookup.trackChanged();
    }
    private static long livePosition() {
        long position=mediaPositionMs+(mediaPaused?0:(long)(Math.max(0,(System.nanoTime()-mediaReceivedNanos)/1_000_000.0)*mediaRate));
        return mediaDurationMs>0?Math.min(position,mediaDurationMs):position;
    }
    public static void poll() {
        long now = System.nanoTime();
        if (now - lastPollNanos < 1_000_000_000L) return;
        lastPollNanos = now;
        try {
            long stamp = Files.isRegularFile(FILE) ? Files.getLastModifiedTime(FILE).toMillis() : -1;
            if (stamp == loadedStamp) return;
            loadedStamp = stamp; timeline = LrcTimeline.EMPTY;
            if (stamp < 0 || Files.size(FILE) > 128 * 1024) return;
            timeline = LrcTimeline.parse(Files.readString(FILE, StandardCharsets.UTF_8));
            startedAt = now;
        } catch (IOException e) { timeline = LrcTimeline.EMPTY; }
    }
    public static String current() {
        poll();
        if (mediaConnected()) {
            LrcTimeline selected=mediaLines.isEmpty()?timeline:mediaLines;
            if(plainMode())return plainLines.get(plainIndex);
            if(!mediaPositionKnown||selected.isEmpty())return "";
            String line=selected.at(livePosition());
            return line==null?"":line;
        }
        return timeline.at(Math.max(0, (System.nanoTime() - startedAt) / 1_000_000L));
    }
    public static float progress() {
        if (mediaConnected()) return mediaPositionKnown?(mediaLines.isEmpty()?timeline:mediaLines).progress(livePosition()):-1;
        return timeline.progress(Math.max(0, (System.nanoTime() - startedAt) / 1_000_000L));
    }
    public static String testLine(String... rows) { return LrcTimeline.parse(String.join("\n", rows)).at(1500); }
}
