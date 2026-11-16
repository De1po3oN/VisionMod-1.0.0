package com.visionmod;

/** Render-rate, critically damped following. Position is player-relative, not a free world entity. */
public final class PetMotion {
    public record Pose(double x,double y,double z,float facingYaw,float roll,float visibility){}
    private static final double RADIUS=1.04,HEIGHT=1.40;
    private final Spring orbit=new Spring(),vertical=new Spring(),facing=new Spring(),tilt=new Spring();
    private boolean initialized;
    private double previousX,previousY,previousZ;
    public void reset(){initialized=false;}
    public Pose sample(double x,double y,double z,double cameraYaw,double seconds,double age,boolean animate,boolean viewChanged){
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Double.isFinite(cameraYaw)){reset();return null;}
        seconds=Double.isFinite(seconds)?Math.max(0,seconds):0;
        age=Double.isFinite(age)?age:0;
        cameraYaw=wrap(cameraYaw);
        double bob=animate?Math.sin(age*2.2)*.04:0,targetY=y+HEIGHT+bob;
        double dx=x-previousX,dy=y-previousY,dz=z-previousZ;
        if(!initialized||!animate||seconds>.25||dx*dx+dy*dy+dz*dz>64){
            orbit.snap(cameraYaw);vertical.snap(targetY);facing.snap(cameraYaw);tilt.snap(0);initialized=true;
        }else{
            if(viewChanged){orbit.snap(cameraYaw);facing.snap(cameraYaw);tilt.snap(0);}
            double dt=Math.min(.1,seconds);
            orbit.angle(cameraYaw,15,dt);facing.angle(cameraYaw,23,dt);
            vertical.step(targetY,16,dt);vertical.bound(targetY-.60,targetY+.60);
            double lag=wrap(cameraYaw-orbit.value);
            tilt.step(Math.clamp(lag*.30,-8,8)+Math.sin(age*1.6)*2,15,dt);
        }
        previousX=x;previousY=y;previousZ=z;
        double angle=Math.toRadians(orbit.value),lag=Math.abs(wrap(cameraYaw-orbit.value));
        // Large instantaneous turns must not sweep a companion across the player's face.
        float visibility=(float)(1-smooth((lag-35)/30));
        return new Pose(x+Math.cos(angle)*RADIUS,vertical.value,z+Math.sin(angle)*RADIUS,
            (float)facing.value,(float)tilt.value,visibility);
    }
    public static float cameraOpacity(double x,double y,double z,double forwardX,double forwardY,double forwardZ){
        double distance=Math.sqrt(x*x+y*y+z*z),depth=x*forwardX+y*forwardY+z*forwardZ;
        if(!Double.isFinite(distance)||!Double.isFinite(depth))return 0;
        return (float)(smooth((distance-.65)/.50)*smooth((depth-.55)/.45));
    }
    private static double smooth(double t){t=Math.clamp(t,0,1);return t*t*(3-2*t);}
    private static double wrap(double angle){double value=angle%360;return value>=180?value-360:value< -180?value+360:value;}
    private static final class Spring {
        double value,velocity;
        void snap(double target){value=target;velocity=0;}
        void step(double target,double speed,double dt){
            double error=value-target,c=(velocity+speed*error)*dt,decay=Math.exp(-speed*dt);
            value=target+(error+c)*decay;velocity=(velocity-speed*c)*decay;
        }
        void angle(double target,double speed,double dt){step(value+wrap(target-value),speed,dt);value=wrap(value);}
        void bound(double min,double max){
            if(value<min){value=min;velocity=Math.max(0,velocity);}
            if(value>max){value=max;velocity=Math.min(0,velocity);}
        }
    }
}
