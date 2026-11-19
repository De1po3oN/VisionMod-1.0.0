package com.visionmod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Immutable, bounded LRC timeline; independent of Minecraft for regression tests. */
public final class LrcTimeline {
    private static final String TIMESTAMP = "(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?";
    private static final Pattern TAG = Pattern.compile("\\[" + TIMESTAMP + "\\]");
    private static final Pattern WORD_TAG = Pattern.compile("<" + TIMESTAMP + ">");
    private static final Pattern OFFSET = Pattern.compile("(?im)^\\s*\\[offset:([+-]?\\d{1,8})\\]\\s*$");
    private record Line(long millis, String text) {}
    public static final LrcTimeline EMPTY = new LrcTimeline(List.of());
    private final List<Line> lines;
    private LrcTimeline(List<Line> lines) { this.lines = List.copyOf(lines); }
    public boolean isEmpty() { return lines.isEmpty(); }
    public int size() { return lines.size(); }
    public boolean hasWords() { return lines.stream().anyMatch(line -> !line.text.isBlank()); }
    public String words() {
        return String.join("\n", lines.stream().map(Line::text).filter(line -> !line.isBlank()).toList());
    }

    public static LrcTimeline parse(String lrc) {
        if (lrc == null || lrc.length() > 200_000) return EMPTY;
        if (lrc.startsWith("\uFEFF")) lrc = lrc.substring(1);
        long offset = 0;
        Matcher offsets = OFFSET.matcher(lrc);
        if (offsets.find()) offset = Long.parseLong(offsets.group(1));
        List<Line> result = new ArrayList<>();
        for (String row : lrc.split("\\R")) {
            String input = row.strip();
            Matcher tags = TAG.matcher(input);
            List<Long> times = new ArrayList<>();
            int end = 0;
            while (tags.find() && tags.start() == end) {
                int seconds = Integer.parseInt(tags.group(2));
                if (seconds > 59) break;
                String fraction = tags.group(3);
                int ms = fraction == null ? 0 : Integer.parseInt((fraction + "00").substring(0, 3));
                times.add(Math.max(0, (Long.parseLong(tags.group(1)) * 60 + seconds) * 1000 + ms + offset));
                end = tags.end();
                while (end < input.length() && (input.charAt(end) == ' ' || input.charAt(end) == '\t')) end++;
                if (times.size() == 32) break;
            }
            if (times.isEmpty()) continue;
            String text = WORD_TAG.matcher(input.substring(end)).replaceAll("")
                .replaceAll("[\\p{Cntrl}§]", "").strip();
            if (text.codePointCount(0, text.length()) > 140) text = text.substring(0, text.offsetByCodePoints(0, 140));
            for (long time : times) {
                result.add(new Line(time, text)); // Blank timed lines intentionally clear old words.
                if (result.size() == 1200) break;
            }
            if (result.size() == 1200) break;
        }
        result.sort(Comparator.comparingLong(Line::millis));
        return new LrcTimeline(result);
    }

    private int index(long position) {
        int low = 0, high = lines.size() - 1, found = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (lines.get(mid).millis <= position) { found = mid; low = mid + 1; }
            else high = mid - 1;
        }
        return found;
    }
    public String at(long position) {
        int i = index(position);
        return i < 0 ? null : lines.get(i).text;
    }
    /** -1 when the line has no known end; do not invent playback progress. */
    public float progress(long position) {
        int i = index(position);
        if (i < 0 || i + 1 >= lines.size()) return -1;
        long duration = lines.get(i + 1).millis - lines.get(i).millis;
        return duration <= 0 ? -1 : Math.clamp((position - lines.get(i).millis) / (float)duration, 0, 1);
    }
}
