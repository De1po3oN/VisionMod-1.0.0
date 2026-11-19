package com.visionmod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Matches a catalog entry without flattening cover, live, acoustic or remix versions. */
public final class LyricsSearch {
    private static final Pattern TITLE_SEPARATOR = Pattern.compile("\\s+(?:-|—|–|\\|)\\s+");
    private static final Pattern FEATURING = Pattern.compile("(?iu)(?:^|\\s+|\\s*[\\[(]\\s*)(?:feat\\.?|ft\\.?|featuring)\\s+");
    private static final Pattern FEATURED_TITLE = Pattern.compile("(?iu)\\s*[\\[(]\\s*(?:feat\\.?|ft\\.?|featuring)\\s+[^\\]\\r\\n)]+[\\])]");
    private static final Pattern ARTIST_SEPARATOR = Pattern.compile("(?iu)(?:\\s+(?:&|x|×)\\s+|\\s*[,;]\\s*)");

    public record Query(String artist, String title, String unsplitTitle) {
        public Query(String artist, String title) { this(artist, title, ""); }
    }

    private LyricsSearch() {}

    public static Query query(String artist, String title) {
        artist = cleanArtist(artist == null ? "" : artist.strip());
        title = cleanTitle(title == null ? "" : title.strip());
        String[] parts = TITLE_SEPARATOR.split(title, 2);
        if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
            String credit = cleanArtist(parts[0]);
            if (artist.isBlank()) return new Query(credit, parts[1].strip(), title);
            if (normalize(credit).equals(normalize(artist))) title = parts[1].strip();
        }
        return new Query(artist, title);
    }

    private static String cleanArtist(String text) {
        return clean(text).replaceAll("(?iu)\\s*[-—–]\\s*(?:topic|official)\\s*$", "")
            .replaceAll("(?i)VEVO$", "").strip();
    }

    private static String cleanTitle(String title) {
        return FEATURED_TITLE.matcher(clean(title)).replaceAll("").strip();
    }

    private static String clean(String text) {
        return text.replaceAll("(?iu)\\s*(?:[—–|·]| - )\\s*(?:яндекс[ .]?музыка|yandex\\s*music|youtube music|spotify)\\s*$", "")
            .replaceAll("(?iu)\\s*[\\[(](?:official(?: music)?(?: (?:video|audio))?(?: (?:hd|4k))?|lyrics?(?: video)?|audio|visuali[sz]er|текст(?: песни)?|официальн(?:ое|ый) (?:видео|клип)|\\d{4} remaster(?:ed)?|remaster(?:ed)?(?: \\d{4})?)[\\])]", "")
            .replaceAll("(?U)\\s+", " ").strip();
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private static String field(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    public static String select(String json, Query query, long durationMs) {
        return selectResult(json, query, durationMs, "LRCLIB").timed();
    }

    public static boolean matches(String title, String artist, Query query) {
        return matchPenalty(title, artist, query) >= 0;
    }

    /** Exact artist credits win a tie. Inexact matches still require a matching primary credit. */
    static int matchPenalty(String title, String artist, Query query) {
        String wantedTitle = normalize(cleanTitle(query.title()));
        if (wantedTitle.isBlank() || !normalize(cleanTitle(title)).equals(wantedTitle)) return -1;
        String actual = normalize(cleanArtist(artist));
        String wanted = normalize(cleanArtist(query.artist()));
        if (wanted.isBlank() || actual.equals(wanted)) return 0;
        List<String> actualCredits = artistCredits(artist);
        List<String> wantedCredits = artistCredits(query.artist());
        if (actualCredits.isEmpty() || wantedCredits.isEmpty()) return -1;
        boolean actualContainsWanted = actualCredits.containsAll(wantedCredits);
        boolean wantedContainsActual = wantedCredits.containsAll(actualCredits);
        if (actualContainsWanted && wantedContainsActual) return 0;
        if (!actualCredits.getFirst().equals(wantedCredits.getFirst())) return -1;
        // Do not match two different collaborations by the primary artist alone.
        return actualContainsWanted || wantedContainsActual ? 1 : -1;
    }

    static String primaryArtistName(String artist) {
        String[] featured = FEATURING.split(cleanArtist(artist), 2);
        return ARTIST_SEPARATOR.split(featured[0], 2)[0].strip();
    }

    private static List<String> artistCredits(String artist) {
        String cleaned = FEATURING.matcher(cleanArtist(artist)).replaceAll(", ");
        return ARTIST_SEPARATOR.splitAsStream(cleaned).map(LyricsSearch::normalize).filter(credit -> !credit.isBlank()).toList();
    }

    /** A known duration is required to agree within five seconds. */
    static double durationDifference(JsonObject song, String key, double duration) {
        if (duration <= 0) return 0;
        try {
            double value = song.get(key).getAsDouble();
            return Double.isFinite(value) && value > 0 ? Math.abs(value - duration) : Double.POSITIVE_INFINITY;
        } catch (RuntimeException ignored) {
            return Double.POSITIVE_INFINITY;
        }
    }

    private record Candidate(JsonObject song, String artist, double difference, int penalty) {}

    public static LyricsResult selectResult(String json, Query query, long durationMs, String provider) {
        JsonElement root = JsonParser.parseString(json);
        List<Candidate> candidates = new ArrayList<>();
        if (root.isJsonObject()) addCandidate(candidates, root, query, durationMs);
        else if (root.isJsonArray()) {
            var entries = root.getAsJsonArray();
            for (int i = 0; i < Math.min(entries.size(), 80); i++) addCandidate(candidates, entries.get(i), query, durationMs);
        }
        double closest = candidates.stream().mapToDouble(Candidate::difference).min().orElse(0);
        if (query.artist().isBlank() && candidates.stream().filter(candidate -> candidate.difference() <= closest + 1.5)
            .map(Candidate::artist).filter(artist -> !artist.isBlank()).distinct().limit(2).count() > 1) return LyricsResult.EMPTY;

        LyricsResult best = LyricsResult.EMPTY;
        double bestScore = Double.POSITIVE_INFINITY;
        for (Candidate candidate : candidates) {
            // With no artist, retain only entries near the closest duration. A cover with words
            // must not displace the likely recording simply because that entry has no text.
            if (query.artist().isBlank() && candidate.difference() > closest + 1.5) continue;
            var words = new LyricsResult(field(candidate.song(), "syncedLyrics"), field(candidate.song(), "plainLyrics"), provider);
            if (!words.hasText()) continue;
            double score = candidate.difference() + candidate.penalty() * 0.5 + (words.timed().isBlank() ? 100 : 0);
            if (score < bestScore) { best = words; bestScore = score; }
        }
        return best;
    }

    private static void addCandidate(List<Candidate> candidates, JsonElement entry, Query query, long durationMs) {
        if (!entry.isJsonObject()) return;
        var song = entry.getAsJsonObject();
        String artist = field(song, "artistName");
        int penalty = matchPenalty(field(song, "trackName"), artist, query);
        if (penalty < 0) return;
        double difference = durationDifference(song, "duration", durationMs / 1000.0);
        if (difference > 5) return;
        candidates.add(new Candidate(song, normalize(cleanArtist(artist)), difference, penalty));
    }
}
