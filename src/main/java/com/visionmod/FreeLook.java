package com.visionmod;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Independent, client-only orbit camera for the third-person view.
 *
 * Left Alt changes only the camera orientation, never the player/entity yaw.
 * Releasing Alt retains the chosen orbit. F5 resets it back to vanilla.
 * All angles are in degrees; optional interpolation only affects the view.
 */
public final class FreeLook {
    public static boolean enabled = true;
    public static boolean smooth = true;
    public static boolean active;
    public static boolean locked;
    public static float yaw;
    public static float pitch;

    private static float renderedYaw;
    private static float renderedPitch;
    private static long lastRenderNanos;
    private static boolean wasHeld;
    private static boolean warnedAboutExternal;
    private static CameraType previousCameraType;

    private FreeLook() { }

    public static void tick(boolean held) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !enabled || !VisionModClient.enabled) {
            stop(true);
            return;
        }

        held &= mc.screen == null;

        // Changing perspective (including F5) explicitly cancels the orbit.
        if (locked && mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            stop(false);
            wasHeld = held;
            return;
        }

        if (held && !wasHeld && !locked) {
            previousCameraType = mc.options.getCameraType();
            yaw = mc.player.getYRot();
            pitch = mc.player.getXRot();
            renderedYaw = yaw;
            renderedPitch = pitch;
            lastRenderNanos = 0L;
            locked = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);

            if (!warnedAboutExternal && (FabricLoader.getInstance().isModLoaded("freelook")
                    || FabricLoader.getInstance().isModLoaded("perspectivereborn"))) {
                warnedAboutExternal = true;
                String warning = "[Vision Mod] External FreeLook detected. Unbind its camera key to avoid conflicts.";
                System.err.println(warning);
                mc.player.displayClientMessage(Component.literal(warning), false);
            }
        }

        active = locked && held;
        wasHeld = held;
    }

    /** Called from MouseHandler; do not mutate the player's rotation. */
    public static void rotate(double dx, double dy) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        double sensitivity = mc.options.sensitivity().get();
        double factor = sensitivity * 0.6 + 0.2;
        double multiplier = factor * factor * factor * 8.0 * 0.15;
        // Large deltas can occur when the game regains mouse focus.
        dx = Mth.clamp(dx, -250.0, 250.0);
        dy = Mth.clamp(dy, -250.0, 250.0);
        yaw = Mth.wrapDegrees(yaw + (float)(dx * multiplier));
        pitch = Mth.clamp(pitch + (float)(dy * multiplier), -89.9f, 89.9f);
    }

    /** Once per rendered Camera.setup; a time-based easing that is frame-rate independent. */
    public static void stepCamera() {
        if (!locked) return;
        long now = System.nanoTime();
        if (!smooth || lastRenderNanos == 0L) {
            renderedYaw = yaw;
            renderedPitch = pitch;
        } else {
            double seconds = Mth.clamp((now - lastRenderNanos) / 1_000_000_000.0, 0.0, 0.1);
            float blend = (float)(1.0 - Math.exp(-22.0 * seconds));
            renderedYaw = Mth.wrapDegrees(renderedYaw + Mth.wrapDegrees(yaw - renderedYaw) * blend);
            renderedPitch += (pitch - renderedPitch) * blend;
        }
        lastRenderNanos = now;
    }

    public static float cameraYaw() { return renderedYaw; }
    public static float cameraPitch() { return renderedPitch; }
    public static boolean isOrbitCamera() { return locked; }

    private static void stop(boolean restorePerspective) {
        if (restorePerspective && locked && previousCameraType != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
                mc.options.setCameraType(previousCameraType);
            }
        }
        active = false;
        locked = false;
        wasHeld = false;
        previousCameraType = null;
        lastRenderNanos = 0L;
    }
}
