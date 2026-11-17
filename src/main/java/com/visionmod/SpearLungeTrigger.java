package com.visionmod;

/** Detect a burst of movement only inside a window armed by an actual spear attack. */
public final class SpearLungeTrigger {
    private int remaining;
    private double previousSpeed;
    public void reset() { remaining = 0; previousSpeed = 0; }
    public void arm(double speed) {
        if (!Double.isFinite(speed) || speed < 0) { reset(); return; }
        remaining = 10; previousSpeed = speed;
    }
    public boolean sample(double speed, boolean eligible) {
        if (!eligible || !Double.isFinite(speed) || speed < 0) { reset(); return false; }
        if (remaining <= 0) return false;
        remaining--;
        boolean launched = speed > .42 && speed - previousSpeed > .18;
        previousSpeed = speed;
        if (launched) remaining = 0;
        return launched;
    }
}
