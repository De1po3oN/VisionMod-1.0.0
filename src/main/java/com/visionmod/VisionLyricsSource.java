package com.visionmod;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    private record TrackIdentity(String provider, String artist, String title, String location) {}
    private static TrackIdentity lastTrack;
    private static LrcTimeline importedTimeline = LrcTimeline.EMPTY;
    private static List<String> importedWords = List.of();
    private static long manualPositionMs, manualStartedAt;
    private static boolean manualClock, manualPlaying;
    private VisionLyricsSource() {}

    public static String fileLocation() { return FILE.toString(); }
    // Stop presenting a stale song after a native reader disconnects.
    public static boolean mediaConnected() {
        return !mediaTitle.isBlank() && System.nanoTime() - mediaReceivedNanos < 10_000_000_000L;
    }
    public static boolean active() { return mediaConnected() || hasImport() || !timeline.isEmpty(); }
    public static String status() {
        if (manualClock) return manualPlaying ? "Ручной отсчёт идёт" : "Ручной отсчёт на паузе";
        if (hasImport()) {
            if (plainMode()) return "Вставленный текст · строка "+(plainIndex+1)+"/"+plainLineCount();
            return "Вставленный LRC · позиция плеера";
        }
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
        String yandexId = YandexLyricsClient.trackId(track.sourceUrl());
        String location = yandexId.isEmpty() ? track.sourceUrl() : "yandex:" + yandexId;
        var identity = new TrackIdentity(track.provider(), track.artist(), track.title(), location);
        boolean changed = !identity.equals(lastTrack) || mediaTitle.isEmpty();
        if (lastTrack != null && !identity.equals(lastTrack)) clearImport();
        lastTrack = identity;
        mediaArtist=track.artist();mediaTitle=track.title();mediaProvider=track.provider();mediaUrl=track.sourceUrl();
        mediaPositionMs=track.positionMs();mediaPaused=track.paused();mediaDurationMs=track.durationMs();
        mediaRate=track.playbackRate();mediaPositionKnown=track.positionKnown();
        mediaReceivedNanos=Math.min(System.nanoTime(),observedNanos);
        if (changed) {
            mediaLines = LrcTimeline.EMPTY;
            plainLines = List.of();
            if (!hasImport()) plainIndex = 0;
            lyricProvider = "";
            VisionLyricsLookup.trackChanged();
        }
    }
    public static void setMediaLyrics(String artist, String title, String lrc) {
        setMediaLyrics(artist,title,new LyricsResult(lrc,"","LRC"));
    }
    public static void setMediaLyrics(String artist,String title,LyricsResult lyrics){
        if(artist.equals(mediaArtist)&&title.equals(mediaTitle)){
            mediaLines=LrcTimeline.parse(lyrics.timed());plainLines=lyrics.plainLines();lyricProvider=lyrics.source();
            if (!hasImport()) plainIndex = 0;
        }
    }
    public static boolean plainMode(){
        return !selectedWords().isEmpty() && (selectedTimeline().isEmpty() || playbackPosition() < 0);
    }
    public static int plainLineCount(){return selectedWords().size();}
    public static int plainLineIndex(){return plainIndex;}
    public static void movePlainLine(int direction){if(plainLineCount()>0)plainIndex=Math.floorMod(plainIndex+direction,plainLineCount());}
    public static void clearMediaTrack() {
        mediaArtist = mediaTitle = mediaProvider = mediaUrl = "";
        mediaLines = LrcTimeline.EMPTY;plainLines=List.of();lyricProvider="";mediaReceivedNanos = 0;
        if (!hasImport()) { plainIndex = 0; useAutomaticClock(); }
        VisionLyricsLookup.trackChanged();
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
    public static boolean importText(String text) {
        if (text == null || text.isBlank() || text.length() > 200_000) return false;
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        LrcTimeline parsed = LrcTimeline.parse(text);
        if (!parsed.isEmpty() && !parsed.hasWords()) return false;
        List<String> words = new LyricsResult("", parsed.isEmpty() ? text : parsed.words(), "").plainLines();
        if (words.isEmpty()) return false;
        importedTimeline = parsed;
        importedWords = words;
        plainIndex = 0;
        if (!mediaConnected()) lastTrack = null;
        useAutomaticClock();
        return true;
    }
    public static boolean hasImport() { return !importedWords.isEmpty(); }
    public static void clearImport() {
        importedTimeline = LrcTimeline.EMPTY;
        importedWords = List.of();
        plainIndex = 0;
        useAutomaticClock();
    }
    private static LrcTimeline selectedTimeline() {
        if (hasImport()) return importedTimeline;
        return mediaConnected() && !mediaLines.isEmpty() ? mediaLines : timeline;
    }
    private static List<String> selectedWords() {
        return hasImport() ? importedWords : mediaConnected() ? plainLines : List.of();
    }
    public static boolean hasTimedLyrics() {
        poll();
        return selectedTimeline().hasWords();
    }
    public static boolean manualClock() { return manualClock; }
    public static boolean manualPlaying() { return manualClock && manualPlaying; }
    public static void useAutomaticClock() { manualClock = false; manualPlaying = false; }
    public static boolean setManualClock(long positionMs, boolean playing) {
        if (positionMs < 0 || positionMs > 60_000_000 || !hasTimedLyrics()) return false;
        manualClock = true;
        manualPlaying = playing;
        manualPositionMs = positionMs;
        manualStartedAt = System.nanoTime();
        return true;
    }
    /** -1 means the active source has no trustworthy playback position. */
    public static long playbackPosition() {
        if (manualClock) {
            return manualPositionMs + (manualPlaying ? (System.nanoTime() - manualStartedAt) / 1_000_000L : 0);
        }
        if (mediaConnected()) return mediaPositionKnown ? livePosition() : -1;
        if (hasImport() || timeline.isEmpty()) return -1;
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000L);
    }
    public static String current() {
        poll();
        if (plainMode()) return selectedWords().get(plainIndex);
        long position = playbackPosition();
        String line = position < 0 ? null : selectedTimeline().at(position);
        return line == null && (hasImport() || mediaConnected()) ? "" : line;
    }
    public static float progress() {
        long position = playbackPosition();
        return position < 0 ? -1 : selectedTimeline().progress(position);
    }
    public static String testLine(String... rows) { return LrcTimeline.parse(String.join("\n", rows)).at(1500); }
}
