package com.visionmod;

/** A short line entrance; song timing always remains the lyric source's responsibility. */
public final class LyricEntrance {
    public record Frame(float opacity, float offset, float scale) { }
    private String text = "";
    private long changedAt;

    public void reset() { text = ""; changedAt = 0; }

    public Frame sample(String current, long now, boolean animate) {
        current = current == null ? "" : current;
        if (!current.equals(text)) { text = current; changedAt = now; }
        if (!animate || current.isBlank()) return new Frame(1, 0, 1);
        double t = Math.clamp((now - changedAt) / 280_000_000.0, 0, 1);
        float remaining = (float)Math.pow(1 - t, 3);
        // Every word is visible on the first frame, including during a seek.
        return new Frame(1 - .48f * remaining, 5 * remaining, 1 - .025f * remaining);
    }
}
