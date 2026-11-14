package com.visionmod;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Optional client helpers. Fast Use repeats a normal vanilla use action with
 * strict rate limits; it cannot speed up server cooldowns or bypass protection.
 */
public final class VisionTools2 {
    public static final String[] NAMES = {"Fast Use", "Custom Models", "Hit Color"};
    private static final boolean[] FLAGS = new boolean[NAMES.length];
    private static final int[] PALETTE = {
        0xFFA89CFF, 0xFFFF7288, 0xFF65E8B4, 0xFFFFD182, 0xFF8BCAFF
    };
    private static int colorIndex;
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
        .resolve("visionmod/tools-2.properties");
    private static int repeatTicks;
    private VisionTools2() { }

    public static int count() { return NAMES.length; }
    public static boolean enabled(int id) {
        return id >= 0 && id < FLAGS.length && FLAGS[id] && VisionModClient.enabled;
    }
    public static boolean selected(int id) {
        return id >= 0 && id < FLAGS.length && FLAGS[id];
    }
    public static int hitColor() { return enabled(2) ? PALETTE[colorIndex] : VisionMenuTheme.PURPLE; }
    public static String hitColorName() {
        return switch (colorIndex) {
            case 1 -> "RED"; case 2 -> "MINT"; case 3 -> "GOLD";
            case 4 -> "BLUE"; default -> "LAVENDER";
        };
    }
    public static void cycleHitColor() {
        colorIndex = (colorIndex + 1) % PALETTE.length;
        save();
    }
    public static void toggle(int id) {
        if (id < 0 || id >= FLAGS.length) return;
        FLAGS[id] = !FLAGS[id];
        if (id == 1) VisionCustomModels.setEnabled(FLAGS[id]);
        save();
    }
    public static void init() {
        load();
        VisionCustomModels.setEnabled(FLAGS[1]);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!enabled(0) || mc.player == null || mc.gameMode == null ||
                mc.screen != null || mc.level == null || !mc.isWindowActive()) {
                repeatTicks = 0;
                return;
            }
            // A bounded repeat action, not instant use: vanilla/server cooldowns
            // still apply. Only use in empty space to avoid accidental block use.
            if (GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS ||
                mc.player.isUsingItem() || mc.player.getMainHandItem().isEmpty() ||
                (mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS)) {
                repeatTicks = 0;
                return;
            }
            if (++repeatTicks >= 7) {
                repeatTicks = 0;
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
        });
    }
    private static void load() {
        if (!Files.isRegularFile(FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
            for (int i = 0; i < FLAGS.length; i++) {
                FLAGS[i] = Boolean.parseBoolean(p.getProperty("tool." + i, "false"));
            }
            colorIndex = Math.floorMod(Integer.parseInt(p.getProperty("hitColor", "0")), PALETTE.length);
        } catch (Exception e) {
            System.err.println("[VisionMod] Tool settings load failed: " + e);
        }
    }
    private static void save() {
        Properties p = new Properties();
        for (int i = 0; i < FLAGS.length; i++) p.setProperty("tool." + i, Boolean.toString(FLAGS[i]));
        p.setProperty("hitColor", Integer.toString(colorIndex));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "Vision Mod optional client tools");
            }
        } catch (Exception e) {
            System.err.println("[VisionMod] Tool settings save failed: " + e);
        }
    }
}
