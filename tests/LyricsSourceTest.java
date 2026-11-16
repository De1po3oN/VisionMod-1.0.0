import com.visionmod.VisionLyricsSource;
import com.visionmod.VisionLyricsLookup;
import java.nio.file.*;
public class LyricsSourceTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Path dir=Path.of(System.getProperty("visionmod.test.config"));
        check(!VisionLyricsSource.mediaConnected(),"no phantom connection");
        check(VisionLyricsSource.current()==null,"empty source has no invented words");
        VisionLyricsSource.receiveMediaTrack("Spotify","Artist","Song",1600,true);
        check(VisionLyricsSource.current().contains("Song"),"title fallback before lookup");
        VisionLyricsSource.setMediaLyrics("Artist","Song","[00:00.00]First\n[00:01.50]Second\n[00:02.00]Third");
        check(VisionLyricsSource.current().equals("Second"),"paused playback position");
        Thread.sleep(100);
        check(VisionLyricsSource.current().equals("Second"),"pause does not advance clock");
        VisionLyricsSource.receiveMediaTrack("Spotify","Artist","Song",0,true);
        check(VisionLyricsSource.current().equals("First"),"seek backwards");
        VisionLyricsSource.receiveMediaTrack("Spotify","Other Artist","Next",0,true);
        check(VisionLyricsSource.current().contains("Next"),"new track clears old lyric");
        VisionLyricsSource.setMediaLyrics("Artist","Song","[00:00]STALE");
        check(!VisionLyricsSource.current().contains("STALE"),"late response cannot replace new song");
        VisionLyricsSource.receiveMediaTrack("Spotify","Other Artist","Next",0,false);
        VisionLyricsSource.setMediaLyrics("Other Artist","Next","[00:00]Start\n[00:00.150]Advance");
        Thread.sleep(200);
        check(VisionLyricsSource.current().equals("Advance"),"playing clock advances");
        VisionLyricsSource.clearMediaTrack();VisionLyricsLookup.clear();
        check(!VisionLyricsSource.mediaConnected(),"off clears media");
        Files.createDirectories(dir.resolve("visionmod"));
        Files.writeString(dir.resolve("visionmod/lyrics.lrc"),"[00:00.00]Offline");
        Thread.sleep(1100);VisionLyricsSource.poll();
        check(VisionLyricsSource.current().equals("Offline"),"offline LRC fallback");
        Files.delete(dir.resolve("visionmod/lyrics.lrc"));
        Thread.sleep(1100);VisionLyricsSource.poll();
        check(VisionLyricsSource.current()==null,"deleted LRC clears old text");
        System.out.println("PASS: "+checks+" lyric source assertions (test scheduler, not Minecraft)");
    }
}
