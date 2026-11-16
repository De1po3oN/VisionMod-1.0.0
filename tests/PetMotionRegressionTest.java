import com.visionmod.PetMotion;

public final class PetMotionRegressionTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static double angle(PetMotion.Pose pose){return Math.toDegrees(Math.atan2(pose.z(),pose.x()));}
    private static PetMotion.Pose turn(int fps){
        PetMotion motion=new PetMotion();motion.sample(0,0,0,0,0,0,true,false);
        PetMotion.Pose pose=null;
        for(int i=0;i<fps;i++)pose=motion.sample(0,0,0,90,1.0/fps,0,true,false);
        return pose;
    }
    public static void main(String[] args){
        PetMotion motion=new PetMotion();
        var idle=motion.sample(0,0,0,0,0,0,true,false);
        check(Math.abs(idle.x()-1.04)<1e-9&&idle.y()==1.4,"initial shoulder position");
        var delayed=motion.sample(0,0,0,90,1.0/60,0,true,false);
        check(angle(delayed)>0&&angle(delayed)<90,"camera turns ease rather than teleport");
        check(delayed.visibility()==0,"large turn cannot sweep a pet across the face");
        for(int i=0;i<75;i++)delayed=motion.sample(0,0,0,90,1.0/60,0,true,false);
        check(Math.abs(angle(delayed)-90)<.01&&Math.abs(delayed.roll())<.05,"pet settles at the new camera-side anchor");
        check(delayed.visibility()==1,"settled pet becomes fully visible");
        for(int i=0;i<75;i++)delayed=motion.sample(0,0,0,0,1.0/60,0,true,false);
        check(Math.abs(angle(delayed))<.01,"pet smoothly returns after turning back");
        var low=turn(30);var normal=turn(60);var high=turn(144);
        check(Math.abs(low.x()-normal.x())<1e-7&&Math.abs(low.z()-high.z())<1e-7,"following is independent of render rate for a fixed target");
        motion.reset();motion.sample(0,0,0,179,0,0,true,false);
        var wrapped=motion.sample(0,0,0,-179,1.0/60,0,true,false);
        check(Math.abs(wrapped.x()-Math.cos(Math.toRadians(179))*1.04)<.01,"yaw wrap does not produce a full orbit");
        motion.reset();motion.sample(0,0,0,0,0,0,true,false);
        double previous=1.4;boolean roseWithLag=false,fellWithLag=false;
        for(int i=1;i<=120;i++){
            double t=i/60.0,y=Math.max(0,8.4*t-16*t*t);
            var pose=motion.sample(0,y,0,0,1.0/60,0,true,false);
            check(Double.isFinite(pose.y())&&Math.abs(pose.y()-(y+1.4))<=.600001,"jump lag stays finite and close to the player");
            check(Math.abs(pose.y()-previous)<.25,"jump/landing movement has no large single-frame step");
            if(t>.05&&t<.2&&pose.y()>1.4&&pose.y()<y+1.3)roseWithLag=true;
            if(t>.35&&t<.6&&pose.y()>y+1.5)fellWithLag=true;
            previous=pose.y();
        }
        check(roseWithLag&&fellWithLag,"both ascent and descent have genuine inertia");
        check(Math.abs(previous-1.4)<.001,"landing settles back to the same height");
        var flipped=motion.sample(0,0,0,180,1.0/60,0,true,true);
        check(flipped.x()< -1&&Math.abs(flipped.z())<.001&&flipped.visibility()==1,"F5 does not replay an orbit through the face");
        var teleported=motion.sample(100,50,100,90,1.0/60,0,true,false);
        check(Math.abs(teleported.x()-100)<.001&&teleported.y()==51.4,"teleport resets follower state");
        check(motion.sample(Double.NaN,0,0,0,.016,0,true,false)==null,"invalid owner position cannot enter the render buffer");
        var disabled=motion.sample(0,0,0,90,.016,99,false,false);
        check(disabled.y()==1.4&&disabled.roll()==0&&Math.abs(disabled.z()-1.04)<1e-9,"animations OFF has a stable anchor");
        check(PetMotion.cameraOpacity(0,0,.2,0,0,1)==0,"camera-close geometry is hidden");
        check(PetMotion.cameraOpacity(1,0,.1,0,0,1)==0,"geometry crossing the near plane is hidden");
        check(PetMotion.cameraOpacity(1,0,4,0,0,1)==1,"normal F5 distance remains visible");
        float fade=PetMotion.cameraOpacity(0,0,.9,0,0,1);
        check(fade>0&&fade<1,"camera clearance fades smoothly");
        check(PetMotion.cameraOpacity(0,0,-3,0,0,1)==0,"nothing is drawn behind the camera");
        System.out.println("PASS: "+checks+" pet motion assertions (camera, jump, frame rate, reset and clearance)");
    }
}
