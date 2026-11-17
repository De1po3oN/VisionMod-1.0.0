package com.visionmod;

/** A short observation window armed exclusively by vanilla's spear lunge action. */
public final class SpearLungeTrigger {
    public record Impulse(double x, double z) { }
    private int remaining;
    private double previousX, previousZ, previousSpeed, forwardX, forwardZ;
    public void reset() { remaining = 0; }
    public void arm(double x, double z, double viewX, double viewZ, double speed) {
        reset();
        double length = Math.hypot(viewX, viewZ);
        if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(length) || length < .001 || !Double.isFinite(speed)) return;
        previousX=x; previousZ=z; previousSpeed=Math.max(0,speed);
        forwardX=viewX/length; forwardZ=viewZ/length; remaining=16;
    }
    public Impulse sample(double x, double z, boolean eligible) {
        if (!eligible || !Double.isFinite(x) || !Double.isFinite(z)) { reset(); return null; }
        if (remaining <= 0) return null;
        remaining--;
        double dx=x-previousX, dz=z-previousZ, speed=Math.hypot(dx,dz);
        previousX=x; previousZ=z;
        if (speed>8) { reset(); return null; } // A teleport is not a spear effect.
        boolean launched=speed>.4 && speed-previousSpeed>.15 && (dx*forwardX+dz*forwardZ)/speed>.65;
        previousSpeed=speed;
        if (!launched) return null;
        reset(); return new Impulse(dx,dz);
    }
}
