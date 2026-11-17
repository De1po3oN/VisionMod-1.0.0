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
        // Public metadata only. A real user's authorized lyrics are never queried by CI.
        var flight=new LyricsHttp.Flight();
        try{
            var reply=LyricsHttp.get(URI.create("https://api.music.yandex.net/search?type=track&page=0&text=Rick%20Astley%20Never%20Gonna%20Give%20You%20Up"),"",flight).get(6,TimeUnit.SECONDS);
            if(reply.code()==200){
                String id=YandexLyricsClient.selectTrack(reply.text(),LyricsSearch.query("Rick Astley","Never Gonna Give You Up"),0);
                System.out.println("[VisionMod][LIVE_YANDEX] Public metadata HTTP 200, matching track="+!id.isEmpty()+"; authenticated lyrics need the user's OAuth token");
            }else System.out.println("[VisionMod][LIVE_YANDEX] Public metadata HTTP "+reply.code()+"; authenticated account playback/lyrics not tested");
        }catch(Exception unavailable){System.out.println("[VisionMod][LIVE_YANDEX] Public endpoint unavailable from this runner; authenticated account playback/lyrics not tested");}
        finally{flight.cancel();}
    }
}
