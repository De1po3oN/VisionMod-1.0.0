package com.visionmod;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/** Read-only public-service check; prints counts, never the song's copyrighted lyrics. */
public final class LiveLyricsSmoke {
    public static void main(String[] args)throws Exception{
        String text=VisionLyricsLookup.fetch(URI.create("https://lrclib.net"),
            LyricsSearch.query("Rick Astley","Never Gonna Give You Up"),0).get(16,TimeUnit.SECONDS);
        LrcTimeline timeline=LrcTimeline.parse(text);
        if(timeline.size()<10)throw new AssertionError("Public lyrics service did not return usable timed lyrics");
        System.out.println("[VisionMod][LIVE_LYRICS] PASS: public LRCLIB returned "+timeline.size()+" timed rows through the production lookup");
    }
}
