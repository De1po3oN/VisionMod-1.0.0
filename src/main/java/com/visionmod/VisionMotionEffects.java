package com.visionmod;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Original, opt-in animations and visual effects. Never changes server-side
 * combat/reach/cooldown or attempts to bypass server-authoritative spear rules.
 */
public final class VisionMotionEffects {
    public static final String[] NAMES = {
        "Swing Animation", "Item Physics", "Spear Lunge FX",
        "Spear Pierce FX", "Camera Streaks"
    };
    private static final boolean[] ENABLED = new boolean[NAMES.length];
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
        .resolve("visionmod/motion-2.properties");
    private static int tickCounter;
    private static int hurtTargetId = -1;
    private static int streakTicks;
    private static float previousYaw;
    private static int effectsCooldown;

    private VisionMotionEffects() {}
    public static int count() { return NAMES.length; }
    public static boolean enabled(int id) {
        return id >= 0 && id < ENABLED.length && ENABLED[id] && VisionModClient.enabled;
    }
    public static boolean selected(int id) {
        return id >= 0 && id < ENABLED.length && ENABLED[id];
    }
    public static void toggle(int id) {
        if (id < 0 || id >= ENABLED.length) return;
        ENABLED[id] = !ENABLED[id];
        save();
    }

    public static void init() {
        load();
        ClientTickEvents.END_CLIENT_TICK.register(VisionMotionEffects::tick);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("visionmod", "camera_streaks"),
            (g, tracker) -> render(g));
    }
    private static boolean holdingSpear(Minecraft mc) {
        return mc.player != null &&
            (mc.player.getMainHandItem().getDescriptionId().contains("spear") ||
             mc.player.getOffhandItem().getDescriptionId().contains("spear"));
    }
    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || !VisionModClient.enabled) {
            tickCounter = 0; effectsCooldown = 0; streakTicks = 0; hurtTargetId = -1;
            if (mc.player != null) previousYaw = mc.player.getYRot();
            return;
        }
        if (effectsCooldown > 0) effectsCooldown--;
        if (streakTicks > 0) streakTicks--;
        if (enabled(4)) {
            float difference = Math.abs(Mth.wrapDegrees(mc.player.getYRot() - previousYaw));
            if (difference > 9) streakTicks = Math.min(7, (int)(difference / 5) + 1);
        }
        previousYaw = mc.player.getYRot();

        if (!holdingSpear(mc) || (!enabled(2) && !enabled(3))) return;

        if (enabled(2) && ++tickCounter >= 4) {
            tickCounter = 0;
            Vec3 velocity = mc.player.getDeltaMovement();
            if (velocity.horizontalDistanceSqr() > 0.12 && mc.player.onGround()) {
                Vec3 behind = mc.player.position().subtract(mc.player.getLookAngle().scale(.55));
                mc.level.addAlwaysVisibleParticle(ParticleTypes.CLOUD,
                    behind.x, behind.y + .14, behind.z,
                    -velocity.x * .12, .015, -velocity.z * .12);
            }
        }
        if (enabled(3) && mc.hitResult instanceof EntityHitResult hit &&
            hit.getEntity() instanceof LivingEntity target && target != mc.player) {
            if (target.hurtTime > 0 && effectsCooldown == 0 &&
                (target.getId() != hurtTargetId || target.hurtTime >= 8)) {
                hurtTargetId = target.getId();
                effectsCooldown = 9;
                Vec3 p = target.getBoundingBox().getCenter();
                for (int i = 0; i < 3; i++) {
                    double a = i * Math.PI * 2 / 3;
                    mc.level.addAlwaysVisibleParticle(ParticleTypes.CRIT,
                        p.x + Math.cos(a) * .28, p.y + .10, p.z + Math.sin(a) * .28,
                        Math.cos(a) * .07, .04, Math.sin(a) * .07);
                }
            }
        }
    }
    private static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled(4) || mc.screen != null || mc.player == null || streakTicks <= 0) return;
        // Original lightweight motion feedback, not a real framebuffer motion blur.
        int w = g.guiWidth(), h = g.guiHeight();
        int alpha = 7 * streakTicks;
        int tint = alpha << 24 | 0x008D76C3;
        int size = 2 + streakTicks / 2;
        g.fill(0, h / 2 - 45, size, h / 2 + 45, tint);
        g.fill(w - size, h / 2 - 45, w, h / 2 + 45, tint);
    }
    private static void load() {
        if (!Files.isRegularFile(FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
            for (int i = 0; i < ENABLED.length; i++)
                ENABLED[i] = Boolean.parseBoolean(p.getProperty("motion." + i,"false"));
        } catch (Exception e) {
            System.err.println("[VisionMod] Motion options load failed: " + e);
        }
    }
    private static void save() {
        Properties p = new Properties();
        for (int i = 0; i < ENABLED.length; i++)
            p.setProperty("motion." + i, Boolean.toString(ENABLED[i]));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "Vision Mod original client motion FX");
            }
        } catch (Exception e) {
            System.err.println("[VisionMod] Motion options save failed: " + e);
        }
    }
}
