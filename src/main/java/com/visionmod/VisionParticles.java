package com.visionmod;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;

/**
 * Explicitly enabled Vision effects must remain visible when the player
 * selects Hide All vanilla particles / MAX FPS. Bounded small amounts only.
 *
 * The marker surrounds the synchronous client particle-creation call.
 * Nested effects restore the previous value in a finally block.
 */
public final class VisionParticles {
    private static boolean creatingOwnParticle;
    private VisionParticles() {}

    public static boolean isOwnParticle() { return creatingOwnParticle; }

    public static void spawn(ClientLevel level, ParticleOptions particle,
        double x, double y, double z, double vx, double vy, double vz) {
        boolean before = creatingOwnParticle;
        creatingOwnParticle = true;
        try {
            level.addAlwaysVisibleParticle(particle, x, y, z, vx, vy, vz);
        } finally {
            creatingOwnParticle = before;
        }
    }
}
