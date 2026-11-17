package com.visionmod;

/** A player-following world plane. Camera position, rotation, FOV and view mode are not inputs. */
public final class WorldLyricMotion {
    public record Pose(double x,double y,double z,float yaw){}
    public static final double DISTANCE=2.6,HEIGHT=1.45;
    private final Spring x=new Spring(),y=new Spring(),z=new Spring(),yaw=new Spring();
    private boolean initialized;
    private double previousX,previousY,previousZ;
    public void reset(){initialized=false;}
    public Pose sample(double ownerX,double ownerY,double ownerZ,double ownerYaw,double seconds,boolean animate){
        if(!Double.isFinite(ownerX)||!Double.isFinite(ownerY)||!Double.isFinite(ownerZ)||!Double.isFinite(ownerYaw)){reset();return null;}
        seconds=Double.isFinite(seconds)?Math.max(0,seconds):0;
        double dx=ownerX-previousX,dy=ownerY-previousY,dz=ownerZ-previousZ;
        if(!initialized||!animate||seconds>.25||dx*dx+dy*dy+dz*dz>64){
            x.snap(ownerX);y.snap(ownerY+HEIGHT);z.snap(ownerZ);yaw.snap(wrap(ownerYaw));initialized=true;
        }else{
            double dt=Math.min(.1,seconds);
            x.step(ownerX,16,dt);y.step(ownerY+HEIGHT,16,dt);z.step(ownerZ,16,dt);
            x.bound(ownerX-1,ownerX+1);y.bound(ownerY+HEIGHT-.6,ownerY+HEIGHT+.6);z.bound(ownerZ-1,ownerZ+1);
            yaw.step(yaw.value+wrap(ownerYaw-yaw.value),13,dt);yaw.value=wrap(yaw.value);
        }
        previousX=ownerX;previousY=ownerY;previousZ=ownerZ;
        double angle=Math.toRadians(yaw.value);
        return new Pose(x.value-Math.sin(angle)*DISTANCE,y.value,z.value+Math.cos(angle)*DISTANCE,(float)yaw.value);
    }
    private static double wrap(double value){value%=360;return value>=180?value-360:value< -180?value+360:value;}
    private static final class Spring {
        double value,velocity;
        void snap(double target){value=target;velocity=0;}
        void step(double target,double speed,double dt){
            double error=value-target,c=(velocity+speed*error)*dt,decay=Math.exp(-speed*dt);
            value=target+(error+c)*decay;velocity=(velocity-speed*c)*decay;
        }
        void bound(double min,double max){
            if(value<min){value=min;velocity=Math.max(0,velocity);}
            if(value>max){value=max;velocity=Math.min(0,velocity);}
        }
    }
}
