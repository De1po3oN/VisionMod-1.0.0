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
    private static boolean mediaPaused;
    private VisionLyricsSource() {}

    public static String fileLocation() { return FILE.toString(); }
    // Background media timers can be throttled; allow a 90 second heartbeat window.
    public static boolean mediaConnected() {
        return !mediaTitle.isBlank() && System.nanoTime() - mediaReceivedNanos < 90_000_000_000L;
    }
    public static boolean active() { return mediaConnected() || !timeline.isEmpty(); }
    public static String status() {
        if (mediaConnected()) return mediaLines.isEmpty() ? "Трек подключён · ищем текст" :
            mediaPaused ? "Пауза" : "Текст синхронизирован";
        return !timeline.isEmpty() ? "Локальный LRC" : "Включи музыку в браузере";
    }
    public static String trackLabel() { return mediaTitle + (mediaArtist.isBlank() ? "" : " — " + mediaArtist); }
    public static String artist() { return mediaArtist; }
    public static String title() { return mediaTitle; }
    public static void receiveMediaTrack(String provider, String artist, String title, long positionMs, boolean paused) {
        boolean changed = !artist.equals(mediaArtist) || !title.equals(mediaTitle) || !provider.equals(mediaProvider);
        mediaArtist = artist; mediaTitle = title; mediaProvider = provider;
        mediaPositionMs = Math.clamp(positionMs, 0, 86_400_000L);
        mediaPaused = paused; mediaReceivedNanos = System.nanoTime();
        if (changed) { mediaLines = LrcTimeline.EMPTY; VisionLyricsLookup.trackChanged(); }
    }
    public static void setMediaLyrics(String artist, String title, String lrc) {
        if (artist.equals(mediaArtist) && title.equals(mediaTitle)) mediaLines = LrcTimeline.parse(lrc);
    }
    public static void clearMediaTrack() {
        mediaArtist = mediaTitle = mediaProvider = "";
        mediaLines = LrcTimeline.EMPTY; mediaReceivedNanos = 0; VisionLyricsLookup.trackChanged();
    }
    private static long livePosition() {
        return mediaPositionMs + (mediaPaused ? 0 : Math.max(0, (System.nanoTime() - mediaReceivedNanos) / 1_000_000L));
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
        if (mediaConnected()) {
            String line = mediaLines.at(livePosition());
            return line == null || line.isBlank() ? "♪ " + trackLabel() : line;
        }
        poll();
        return timeline.at(Math.max(0, (System.nanoTime() - startedAt) / 1_000_000L));
    }
    public static float progress() {
        if (mediaConnected()) return mediaLines.progress(livePosition());
        return timeline.progress(Math.max(0, (System.nanoTime() - startedAt) / 1_000_000L));
    }
    public static String testLine(String... rows) { return LrcTimeline.parse(String.join("\n", rows)).at(1500); }
}
