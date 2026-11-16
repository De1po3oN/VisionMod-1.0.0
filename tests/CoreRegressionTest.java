import com.visionmod.LrcTimeline;
import com.visionmod.SystemMediaTrack;
import com.visionmod.SystemMediaReader;
import com.visionmod.DashboardLayout;
import com.visionmod.PanelLayout;
import com.visionmod.EffectMath;
import com.visionmod.BoundedLyricsBody;

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
                check(layout.cardLeft(i)+layout.cardWidth()<=layout.width()-16,"card within panel");
                check(layout.cardTop(i)+60<=layout.navigationTop()-4,"cards do not overlap navigation");
                if(i%layout.columns()>0)check(layout.cardLeft(i-1)+layout.cardWidth()<layout.cardLeft(i),"columns do not overlap");
            }
        }
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
        if(args.length>0&&args[0].equals("--mpris")){
            final boolean[] observed={false};
            try(var reader=new SystemMediaReader()){
                reader.read(t->{
                    if(t==null)return;
                    observed[0]=true;
                    check(t.title().equals("Integration Song")&&t.artist().equals("Integration Artist"),"real D-Bus roundtrip metadata");
                    check(t.positionMs()==12500&&!t.paused(),"real D-Bus roundtrip position");
                    reader.close();
                });
            }
            check(observed[0],"test service was actually observed");
        }
        System.out.println("PASS: "+checks+" core assertions");
    }
}
