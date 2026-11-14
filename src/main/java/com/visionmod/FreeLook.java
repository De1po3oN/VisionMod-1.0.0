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
 * Releasing Alt immediately restores the previous perspective and clears orbit.
 * F5 remains a vanilla perspective key.
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
    // Log the first real input and camera use of the hook, not a claim
    // that a successful Gradle build means the user's feature is working.
    private static boolean cameraHookObserved;
    private static boolean mouseHookObserved;
    private static int startsLogged;

    private FreeLook() { }

    public static void tick(boolean held) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !enabled || !VisionModClient.enabled) {
            stop(true);
            return;
        }

        held &= mc.screen == null;

        // Hold-only camera: release IMMEDIATELY restores the original perspective.
        // Do not keep the orbit locked until F5; the user explicitly rejected that.
        // Camera.setup and MouseHandler also poll this before rendering/turning,
        // so release is observed at frame rate, not just at 20 client ticks.
        if (!held) {
            if (locked) stop(true);
            wasHeld = false;
            active = false;
            return;
        }

        // F5 pressed during a hold: vanilla perspective wins; do not grab it
        // again until the Alt key has actually been released.
        if (locked && mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            stop(false);
            wasHeld = true;
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
            if (startsLogged++ < 3) {
                System.out.println("[VisionMod][FreeLook] Activated. Original perspective="
                    + previousCameraType + ", target=THIRD_PERSON_BACK");
            }

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

    /**
     * MouseHandler runs during rendered frames, not just at 20 Hz client tick.
     * Activate the camera before handling the first held mouse movement so
     * the vanilla player rotation cannot leak through for one frame.
     */
    public static void syncFromMouse() {
        Minecraft mc = Minecraft.getInstance();
        tick(VisionModClient.enabled &&
             VisionHotkeys.holding(VisionHotkeys.visual(6), mc));
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
        // Match vanilla's independent horizontal/vertical inversion options.
        if (mc.options.invertMouseX().get()) dx = -dx;
        if (mc.options.invertMouseY().get()) dy = -dy;
        yaw = Mth.wrapDegrees(yaw + (float)(dx * multiplier));
        pitch = Mth.clamp(pitch + (float)(dy * multiplier), -89.9f, 89.9f);
        if (!mouseHookObserved) {
            mouseHookObserved = true;
            System.out.println("[VisionMod][FreeLook] Mouse movement intercepted; player orientation preserved");
        }
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

    public static void cameraHookApplied() {
        if (!cameraHookObserved) {
            cameraHookObserved = true;
            System.out.println("[VisionMod][FreeLook] Camera.setup rotation hook reached in third person");
        }
    }

    public static float cameraYaw() { return renderedYaw; }
    public static float cameraPitch() { return renderedPitch; }
    public static boolean isOrbitCamera() { return locked; }

    public static void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (!enabled) stop(true);
        VisionExtras.save();
    }

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
        yaw = pitch = renderedYaw = renderedPitch = 0.0f;
    }
}
