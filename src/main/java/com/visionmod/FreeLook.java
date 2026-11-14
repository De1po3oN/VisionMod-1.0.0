package com.visionmod;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Third-person orbit camera: mouse moves the camera, not the player's aim.
 * After the hold key is released the chosen view angle stays locked.
 * F5 (or changing the game's perspective) restores ordinary camera control.
 */
public final class FreeLook {
    public static boolean enabled = true;
    /** True only while the Free Look hotkey is held. */
    public static boolean active;
    /** True while the third-person camera remains independent from player yaw. */
    public static boolean locked;
    public static float yaw;
    public static float pitch;

    private static boolean wasHeld;
    private static boolean warnedAboutExternal;

    private FreeLook() { }

    public static void tick(boolean held) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null || !enabled || !VisionModClient.enabled) {
            stop();
            return;
        }

        // Do not capture a fresh key press while a settings/chat screen is open.
        held = held && mc.screen == null;

        // The user pressed F5 or changed the view in settings: never fight it.
        if (locked && mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            locked = false;
            active = false;
            wasHeld = held;
            return;
        }

        if (held && !wasHeld && !locked) {
            // Initialise once. Releasing the key must not restore the player's
            // yaw or teleport the camera to its previous view direction.
            yaw = mc.player.getYRot();
            pitch = mc.player.getXRot();
            locked = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);

            if (!warnedAboutExternal &&
                (FabricLoader.getInstance().isModLoaded("freelook") ||
                 FabricLoader.getInstance().isModLoaded("perspectivereborn"))) {
                warnedAboutExternal = true;
                String message = "[Vision Mod] Another FreeLook mod is installed. "
                    + "Unbind its camera key while testing Vision Mod's Left Alt orbit.";
                System.err.println(message);
                mc.player.displayClientMessage(Component.literal(message), false);
            }
        }

        active = locked && held;
        wasHeld = held;
    }

    /** Used by the mouse hook, only while holding the configured hotkey. */
    public static void rotate(double dx, double dy) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        double slider = mc.options.sensitivity().get();
        double factor = slider * 0.6d + 0.2d;
        double multiplier = factor * factor * factor * 8d * 0.15d;

        yaw += (float) (dx * multiplier);
        pitch = Math.max(-90.0f, Math.min(90.0f,
            pitch + (float) (dy * multiplier)));
    }

    /** Camera orientation remains fixed until F5 or module/world reset. */
    public static boolean isOrbitCamera() {
        return locked;
    }

    private static void stop() {
        active = false;
        locked = false;
        wasHeld = false;
    }
}
