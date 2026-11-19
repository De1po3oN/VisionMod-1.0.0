import com.visionmod.LrcTimeline;
import com.visionmod.SystemMediaTrack;
import com.visionmod.SystemMediaReader;
import com.visionmod.DashboardLayout;
import com.visionmod.PanelLayout;
import com.visionmod.EffectMath;
import com.visionmod.BoundedLyricsBody;
import com.visionmod.HudPacking;
import com.visionmod.HotbarTween;
import com.visionmod.FastScrollGesture;

public class CoreRegressionTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        var lrc=LrcTimeline.parse("[00:02.50]Second\n[00:00.00]First\n[00:04.00]\n[00:06.00]Last");
        check(lrc.size()==4,"timed blank preserved");
        check(lrc.at(-1)==null,"no early line");
        check(lrc.at(2499).equals("First"),"first line boundary");
        check(lrc.at(2500).equals("Second"),"next line boundary");
        check(lrc.at(4000).equals(""),"instrumental clears lyric");
        check(lrc.at(6000).equals("Last"),"last line");
        check(lrc.progress(1250)==.5f,"real line progress");
        check(lrc.progress(9000)==-1,"no fake progress after last line");
        var offset=LrcTimeline.parse("[offset:-500]\n[00:01.0][00:02.00]Repeat\n[00:03.125]End");
        check(offset.size()==3,"multi timestamp expansion");
        check(offset.at(499)==null&&offset.at(500).equals("Repeat"),"offset and tenths");
        check(offset.at(2625).equals("End"),"millisecond precision");
        check(LrcTimeline.parse("[00:99.00]Invalid\n[ar:Artist]").isEmpty(),"invalid time ignored");
        check(LrcTimeline.parse("[00:00.00]§cClean\ttext").at(0).equals("cCleantext"),"control text sanitized");
        check(LrcTimeline.parse("x".repeat(200001)).isEmpty(),"input limit");
        check(LrcTimeline.parse("[00:00.00]A\n".repeat(1300)).size()==1200,"timeline limit");
        check(LrcTimeline.parse(null).isEmpty(),"null input");
        var track=SystemMediaTrack.fromMpris("Test","({'xesam:title': <'Песня'>, 'xesam:artist': <['Artist']>, 'PlaybackStatus': <'Playing'>, 'Position': <int64 12500000>},)");
        check(track.title().equals("Песня")&&track.artist().equals("Artist"),"unicode MPRIS metadata");
        check(track.positionMs()==12500&&!track.paused(),"MPRIS microseconds and playing");
        var quotes=SystemMediaTrack.fromMpris("Test","{'xesam:title': <\"Don't stop\">, 'xesam:artist': <['A\\'B']>, 'PlaybackStatus': <'Paused'>}");
        check(quotes.title().equals("Don't stop")&&quotes.artist().equals("A'B")&&quotes.paused(),"GVariant quoting and pause");
        check(SystemMediaTrack.fromMpris("Test","{}").title().isBlank(),"missing session safe");
        var local=SystemMediaTrack.fromMpris("Test","{'xesam:url': <'file:///music/A%20Song.flac'>, 'xesam:albumArtist': <['Album Artist']>, 'PlaybackStatus': <'Paused'>}");
        check(local.title().equals("A Song")&&local.artist().equals("Album Artist"),"untagged local file uses filename and album artist");
        check(SystemMediaTrack.fromMpris("Test","{'xesam:title': <'Last song'>, 'PlaybackStatus': <'Stopped'>}").title().isBlank(),"stopped players do not retain their previous track");
        var unknownPosition=new SystemMediaTrack("YandexMusic","Track","Artist",0,false,0,1,false);
        check(track.selectionScore()>unknownPosition.selectionScore(),"a playing session with position supports synchronization");
        var body=new BoundedLyricsBody();
        final boolean[] cancelled={false};
        body.onSubscribe(new java.util.concurrent.Flow.Subscription(){public void request(long n){}public void cancel(){cancelled[0]=true;}});
        body.onNext(java.util.List.of(java.nio.ByteBuffer.wrap(new byte[200001])));
        check(cancelled[0]&&body.getBody().toCompletableFuture().isCompletedExceptionally(),"oversized HTTP body cancelled");
        var small=new BoundedLyricsBody();
        small.onSubscribe(new java.util.concurrent.Flow.Subscription(){public void request(long n){}public void cancel(){}});
        small.onNext(java.util.List.of(java.nio.ByteBuffer.wrap("lyrics".getBytes(java.nio.charset.StandardCharsets.UTF_8))));small.onComplete();
        check(new String(small.getBody().toCompletableFuture().join()).equals("lyrics"),"HTTP chunks collected");
        for(int[] viewport:new int[][]{{320,240},{426,240},{640,360},{854,480},{960,540},{1920,1080}}){
            var layout=DashboardLayout.of(viewport[0],viewport[1]);
            for(int i=0;i<layout.cards();i++){
                check(layout.cardWidth()>=140,"readable card width");
                check(layout.cardHeight()>=52,"readable card height");
                check(layout.cardLeft(i)+layout.cardWidth()<=layout.width()-16,"card within panel");
                check(layout.cardTop(i)+layout.cardHeight()<=layout.navigationTop()-4,"cards do not overlap navigation");
                if(i%layout.columns()>0)check(layout.cardLeft(i-1)+layout.cardWidth()<layout.cardLeft(i),"columns do not overlap");
            }
            for(int i=0;i<7;i++)check(layout.tabTop(i)+layout.tabHeight()<layout.masterTop(),"navigation does not overlap master toggle");
            check(layout.contentLeft()+64+40<=layout.settingsLeft(),"pagination and settings do not overlap");
            check(layout.settingsLeft()+layout.settingsWidth()<layout.themeLeft(),"footer actions do not overlap");
        }
        check(DashboardLayout.of(320,240).cards()>=2,"minimum GUI shows at least two modules");
        check(DashboardLayout.of(426,240).cards()>=4,"standard small GUI shows at least four modules");
        for(float progress:new float[]{-1,0,.1f,.5f,.9f,1,2,Float.NaN,Float.POSITIVE_INFINITY}){
            check(Float.isFinite(EffectMath.ease(progress)),"finite animation curve");
            check(EffectMath.fade(progress)>=0&&EffectMath.fade(progress)<=1,"bounded fade");
            check(EffectMath.ringRadius(progress,1.05f)>=.18f&&EffectMath.ringRadius(progress,1.05f)<=1.24f,"bounded ring radius");
        }
        check(EffectMath.ease(0)==0&&EffectMath.ease(1)==1,"exact ease endpoints");
        check(EffectMath.blend(0xFF123456,0xFFABCDEF,0)==0xFF123456,"gradient start");
        check(EffectMath.blend(0xFF123456,0xFFABCDEF,1)==0xFFABCDEF,"gradient end");
        check(EffectMath.smooth(0,1,Float.NaN)==0,"invalid frame time cannot poison HUD");
        for(int[] viewport:new int[][]{{320,240},{426,240},{640,360},{854,480},{960,540},{1920,1080}}){
            var panel=PanelLayout.of(viewport[0],viewport[1]);
            for(int i=0;i<panel.pageSize();i++){
                check(panel.rowY(i)+37<=panel.bodyBottom(),"editor cards stay above footer");
                check(panel.rowX(i)+panel.rowWidth()<=panel.x()+panel.width()-16,"editor cards inside panel");
            }
        }
        var armor=new HudPacking.Rect(271,18,144,110);
        var lyric=HudPacking.find(280,25,130,27,427,240,java.util.List.of(armor));
        check(!lyric.overlaps(armor),"lyrics avoid armor panel at small GUI size");
        check(lyric.x()>=0&&lyric.y()>=0&&lyric.x()+130<=427&&lyric.y()+27<=240,"packed panel stays reachable");
        var alone=HudPacking.find(17,23,80,30,427,240,java.util.List.of());
        check(alone.x()==17&&alone.y()==23,"free preferred position remains stable");
        var crowded=HudPacking.find(5,5,80,30,100,50,java.util.List.of(new HudPacking.Rect(0,0,100,50)));
        check(crowded.x()>=0&&crowded.y()>=0&&crowded.x()+80<=100&&crowded.y()+30<=50,"impossible packing still stays on screen");
        var hotbar=new HotbarTween();
        check(hotbar.sample(0,0,true)==0,"hotbar initial slot snaps");
        check(hotbar.sample(6,0,true)==0,"hotbar retarget has no jump");
        float midway=hotbar.sample(6,70_000_000,true);
        check(midway>0&&midway<120,"hotbar passes through intermediate positions");
        check(hotbar.sample(2,70_000_000,true)==midway,"rapid scroll preserves continuity");
        check(hotbar.sample(2,210_000_000,true)==40,"retarget ends at actual selected item");
        check(hotbar.sample(8,220_000_000,false)==160,"disabled animation snaps without changing inventory");
        hotbar.reset();check(hotbar.sample(3,300_000_000,true)==60,"world change clears old slot motion");
        var speed=SystemMediaTrack.fromMpris("Test","{'xesam:title': <'Track'>, 'Position': <int64 1500000>, 'mpris:length': <int64 200000000>, 'Rate': <1.5>, 'PlaybackStatus': <'Playing'>}");
        check(speed.positionKnown()&&speed.durationMs()==200000&&speed.playbackRate()==1.5,"native duration and speed decoded");
        check(!SystemMediaTrack.fromMpris("Test","{}").positionKnown(),"missing native position is not invented");
        var gesture=new FastScrollGesture();
        gesture.beginDrag(1);
        check(!gesture.visit(1),"drag does not repeat the initial handled shift-click");
        check(gesture.visit(2)&&!gesture.visit(2),"one quick move per visited slot");
        check(gesture.visit(3)&&!gesture.visit(2),"returning across a slot does not send duplicate transfers");
        gesture.reset();check(gesture.visit(2),"new drag can revisit a previous slot");
        check(!gesture.scroll(4,.4)&&!gesture.scroll(4,.4)&&gesture.scroll(4,.2),"fractional trackpad deltas accumulate on one slot");
        check(!gesture.scroll(4,.6)&&!gesture.scroll(5,.5),"partial wheel motion cannot transfer a different slot");
        check(!gesture.scroll(5,-.5)&&gesture.scroll(5,-.5),"wheel direction reversal discards old partial motion");
        check(!gesture.scroll(5,Double.NaN)&&!gesture.scroll(-1,1),"invalid wheel input cannot trigger a transfer");
        if(args.length>0&&args[0].equals("--mpris")){
            final boolean[] observed={false};
            try(var reader=new SystemMediaReader()){
                reader.read(t->{
                    if(t==null)return;
                    observed[0]=true;
                    check(t.title().equals("Integration Song")&&t.artist().equals("Integration Artist"),"real D-Bus roundtrip metadata");
                    check(t.positionKnown()&&t.positionMs()==12500&&!t.paused(),"real D-Bus position fallback with oversized pipe output");
                    reader.close();
                });
            }
            check(observed[0],"test service was actually observed");
        }
        System.out.println("PASS: "+checks+" core assertions");
    }
}
