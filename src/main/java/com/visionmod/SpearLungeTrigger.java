package com.visionmod;

/** Compare velocity immediately before and after vanilla's lunge method. */
public final class SpearLungeTrigger {
    public record Impulse(double x, double z) { }
    private boolean armed;
    private double beforeX, beforeZ;
    public void reset() { armed = false; }
    public void arm(double x, double z) {
        armed = Double.isFinite(x) && Double.isFinite(z);
        beforeX = x; beforeZ = z;
    }
    public Impulse finish(double x, double z, boolean eligible) {
        boolean pending = armed; reset();
        if (!pending || !eligible || !Double.isFinite(x) || !Double.isFinite(z)) return null;
        double dx = x - beforeX, dz = z - beforeZ;
        return dx * dx + dz * dz > .0001 ? new Impulse(dx, dz) : null;
    }
}
