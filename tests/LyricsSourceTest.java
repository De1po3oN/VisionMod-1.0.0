import com.visionmod.VisionLyricsSource;
import com.visionmod.VisionLyricsLookup;
import com.visionmod.SystemMediaTrack;
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
        VisionLyricsSource.setMediaLyrics("Other Artist","Next","[00:00]Start\n[00:00.150]Advance\n[00:00.250]\n[00:00.500]End");
        VisionLyricsSource.receiveMediaTrack("Spotify","Other Artist","Next",300,true);
        check(VisionLyricsSource.current().isEmpty(),"instrumental gap remains silent rather than cycling manual words");
        VisionLyricsSource.receiveMediaTrack(new SystemMediaTrack("Spotify","Next","Other Artist",0,false,1000,2,true),System.nanoTime()-100_000_000);
        check(VisionLyricsSource.current().equals("Advance"),"capture timestamp and playback rate survive client queue delay");
        VisionLyricsSource.receiveMediaTrack(new SystemMediaTrack("Spotify","Next","Other Artist",0,false,1000,1,false),System.nanoTime());
        check(VisionLyricsSource.current().contains("Next")&&VisionLyricsSource.progress()<0,"missing player position does not claim sync");
        VisionLyricsSource.clearMediaTrack();VisionLyricsLookup.clear();
        check(!VisionLyricsSource.mediaConnected(),"off clears media");
        Files.createDirectories(dir.resolve("visionmod"));
        Files.writeString(dir.resolve("visionmod/lyrics.lrc"),"[00:00.00]Offline");
        Thread.sleep(1100);VisionLyricsSource.poll();
        check(VisionLyricsSource.current().equals("Offline"),"offline LRC fallback");
        Files.writeString(dir.resolve("visionmod/lyrics.lrc"),"[00:00.00]Offline\n[00:01.00]Positioned");
        Thread.sleep(1100);VisionLyricsSource.poll();
        VisionLyricsSource.receiveMediaTrack("Spotify","Artist","Song",1500,true);
        check(VisionLyricsSource.current().equals("Positioned"),"local LRC uses native song position while music plays");
        VisionLyricsSource.clearMediaTrack();
        Files.delete(dir.resolve("visionmod/lyrics.lrc"));
        Thread.sleep(1100);VisionLyricsSource.poll();
        check(VisionLyricsSource.current()==null,"deleted LRC clears old text");
        System.out.println("PASS: "+checks+" lyric source assertions (test scheduler, not Minecraft)");
    }
}
