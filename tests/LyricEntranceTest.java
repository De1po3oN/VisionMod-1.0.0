import com.visionmod.LyricEntrance;

public final class LyricEntranceTest {
    public static void main(String[] args) {
        var motion = new LyricEntrance();
        long t = 4_000_000_000L;
        var first = motion.sample("First line", t, true);
        assert first.opacity() >= .5f : "Lyrics must be visible immediately";
        var settled = motion.sample("First line", t + 300_000_000L, true);
        assert settled.opacity() == 1 && settled.offset() == 0 && settled.scale() == 1;
        var seek = motion.sample("After seek", t + 310_000_000L, true);
        assert seek.opacity() == first.opacity() : "Seeking must restart only the visual transition";
        var staticFrame = motion.sample("After seek", t + 320_000_000L, false);
        assert staticFrame.opacity() == 1 && staticFrame.offset() == 0 && staticFrame.scale() == 1;
        float previous = 0;
        for (long step = 0; step <= 300_000_000L; step += 10_000_000L) {
            var frame = motion.sample("After seek", t + 310_000_000L + step, true);
            assert frame.opacity() >= previous && frame.opacity() <= 1;
            assert frame.offset() >= 0 && frame.offset() <= 5;
            previous = frame.opacity();
        }
        motion.sample("", t + 1_000_000_000L, true);
        assert motion.sample("After seek", t + 2_000_000_000L, true).opacity() == first.opacity();
        motion.reset();
        assert motion.sample("After seek", t, true).opacity() == first.opacity();
        System.out.println("[VisionMod][LYRIC_ENTRANCE] PASS: immediate text, seek, blank intervals, monotonic settling, animations off");
    }
}
