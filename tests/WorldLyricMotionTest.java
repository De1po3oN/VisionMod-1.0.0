import com.visionmod.WorldLyricMotion;

public final class WorldLyricMotionTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static WorldLyricMotion.Pose settle(int fps){
        var motion=new WorldLyricMotion();motion.sample(0,0,0,0,0,true);
        WorldLyricMotion.Pose pose=null;
        for(int i=0;i<fps;i++)pose=motion.sample(.3,.3,.3,80,1.0/fps,true);
        return pose;
    }
    public static void main(String[] args){
        var motion=new WorldLyricMotion();
        var pose=motion.sample(10,20,30,0,0,true);
        check(pose.x()==10&&pose.y()==21.45&&Math.abs(pose.z()-32.6)<1e-10,"plane starts ahead of the player in world coordinates");
        var turn=motion.sample(10,20,30,90,1.0/60,true);
        check(turn.yaw()>0&&turn.yaw()<10&&turn.x()<10&&turn.z()>32,"turning has small inertial world motion");
        for(int i=0;i<100;i++)turn=motion.sample(10,20,30,90,1.0/60,true);
        check(Math.abs(turn.x()-7.4)<.001&&Math.abs(turn.z()-30)<.001,"fixed physical distance after settling");
        var moved=motion.sample(11,21,31,90,1.0/60,true);
        check(moved.x()>7.4&&moved.x()<8.4&&moved.y()>21.45&&moved.y()<22.45,"translation and jump follow smoothly");
        for(int i=0;i<100;i++)moved=motion.sample(11,21,31,90,1.0/60,true);
        check(Math.abs(moved.y()-22.45)<.001,"vertical motion settles");
        var landed=motion.sample(11,20,31,90,1.0/60,true);
        check(landed.y()>21.45&&landed.y()<22.45,"descent retains inertia");
        var low=settle(30);var normal=settle(60);var high=settle(144);
        check(Math.abs(low.x()-normal.x())<1e-8&&Math.abs(low.z()-high.z())<1e-8&&Math.abs(low.y()-high.y())<1e-8,"render rate does not change world following");
        motion.reset();motion.sample(0,0,0,179,0,true);
        var wrapped=motion.sample(0,0,0,-179,.016,true);
        check(Math.abs(Math.abs(wrapped.yaw())-180)<1,"short rotation across the yaw seam");
        var teleported=motion.sample(100,50,100,-90,.016,true);
        check(Math.abs(teleported.x()-102.6)<1e-8&&teleported.y()==51.45,"teleport resets the world plane");
        var noAnimation=motion.sample(0,0,0,90,.016,false);
        check(Math.abs(noAnimation.x()+2.6)<1e-8&&noAnimation.y()==1.45,"animations OFF uses a fixed player-relative plane");
        check(motion.sample(Double.NaN,0,0,0,.016,true)==null,"invalid owner cannot reach the GPU");
        motion.reset();motion.sample(0,0,0,0,0,true);
        var paused=motion.sample(0,0,0,90,.5,true);
        check(paused.yaw()==90,"long render gaps reset stale inertia");
        System.out.println("PASS: "+checks+" world lyric motion assertions (physical anchor, translation, turn, jump, frame rate and reset)");
    }
}
