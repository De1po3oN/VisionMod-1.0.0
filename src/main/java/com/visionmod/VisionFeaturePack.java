package com.visionmod;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Vision 2.0's lightweight ORIGINAL HUD features.
 *
 * Not a port of the proprietary Fading Light classes or their resources.
 * All options are off by default. Nearby-entity scans happen at most every ten
 * client ticks and no persistent per-entity allocations are retained.
 */
public final class VisionFeaturePack {
    public static final String[] NAMES = {
        "Aspect Bars", "Armor Colorful", "Target Health", "Hit Range",
        "TNT Indicator", "Arrow Names", "Hit Pulse", "Blood Hit"
    };
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir()
        .resolve("visionmod/features-2.properties");
    private static final boolean[] ENABLED = new boolean[NAMES.length];
    private static final EquipmentSlot[] ARMOR = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final String[] ARMOR_NAMES = {"Helmet", "Chest", "Legs", "Boots"};
    private static int ticks;
    private static int nearestTnt = -1;
    private static int nearestArrow = -1;
    private static int hitPulse;
    private static int lastHitSequence;

    private VisionFeaturePack() { }

    public static int count() { return NAMES.length; }
    public static boolean enabled(int i) { return i >= 0 && i < ENABLED.length && ENABLED[i]; }
    public static void toggle(int i) {
        if (i < 0 || i >= ENABLED.length) return;
        ENABLED[i] = !ENABLED[i];
        save();
    }
    public static void init() {
        load();
        ClientTickEvents.END_CLIENT_TICK.register(VisionFeaturePack::tick);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("visionmod", "feature_pack"),
            (g, tracker) -> render(g));
    }

    private static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            ticks = 0;
            nearestTnt = -1;
            nearestArrow = -1;
            lastHitSequence = VisionCombatTracker.hitSequence();
            hitPulse = 0;
            return;
        }
        if (!VisionModClient.enabled) return;
        if (hitPulse > 0) hitPulse--;
        // Fire exactly once per client-observed hurt event after our own attack,
        // even when crosshair has moved away by the time the target reacts.
        int sequence = VisionCombatTracker.hitSequence();
        if (sequence != lastHitSequence) {
            lastHitSequence = sequence;
            if (ENABLED[6] || ENABLED[7]) hitPulse = 8;
        }
        if (!ENABLED[6] && !ENABLED[7]) hitPulse = 0;

        // Avoid scanning the player-dense world every rendered frame.
        if (++ticks < 10) return;
        ticks = 0;
        nearestTnt = -1;
        nearestArrow = -1;
        if (!ENABLED[4] && !ENABLED[5]) return;
        double tntDist = Double.MAX_VALUE, arrowDist = Double.MAX_VALUE;
        List<Entity> nearby = mc.level.getEntitiesOfClass(Entity.class,
            mc.player.getBoundingBox().inflate(32.0),
            e -> e.isAlive() && (e.getType() == EntityType.TNT ||
                e.getType() == EntityType.ARROW || e.getType() == EntityType.SPECTRAL_ARROW));
        for (Entity entity : nearby) {
            double d = entity.distanceToSqr(mc.player);
            if (ENABLED[4] && entity.getType() == EntityType.TNT && d < tntDist) {
                tntDist = d;
                nearestTnt = entity.getId();
            }
            if (ENABLED[5] && (entity.getType() == EntityType.ARROW ||
                entity.getType() == EntityType.SPECTRAL_ARROW) && d < arrowDist) {
                arrowDist = d;
                nearestArrow = entity.getId();
            }
        }
    }

    private static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!VisionModClient.enabled || mc.level == null || mc.player == null || mc.screen != null) return;
        int w = g.guiWidth(), h = g.guiHeight();
        if (ENABLED[0]) {
            int bar = Math.max(0, (int)Math.round((h - w / 2.39) / 2.0));
            if (bar > 0) {
                g.fill(0, 0, w, bar, 0xF0000000);
                g.fill(0, h - bar, w, h, 0xF0000000);
            }
        }
        if (ENABLED[1]) {
            int x = w - 126, y = 24;
            g.fill(x - 6, y - 6, w - 5, y + 61, 0xBA151324);
            g.drawString(mc.font, "ARMOR COLORFUL", x, y, VisionMenuTheme.CYAN);
            int line = 0;
            for (int i = 0; i < ARMOR.length; i++) {
                ItemStack item = mc.player.getItemBySlot(ARMOR[i]);
                if (item.isEmpty()) continue;
                int value = !item.isDamageableItem() ? 100 :
                    (int)Math.round(100.0 * Math.max(0, item.getMaxDamage() - item.getDamageValue())
                        / Math.max(1, item.getMaxDamage()));
                int color = value > 65 ? 0xFF7CECC1 : value > 30 ? 0xFFFFD487 : 0xFFFF758F;
                g.drawString(mc.font, ARMOR_NAMES[i] + ": " + value + "%", x, y + 12 + 11 * line++, color);
            }
        }

        LivingEntity selected = mc.hitResult instanceof EntityHitResult result &&
            result.getEntity() instanceof LivingEntity living ? living : null;
        if (selected != null && (ENABLED[2] || ENABLED[3])) {
            int x = w / 2 - 70, y = h / 2 + 35;
            g.fill(x - 5, y - 5, x + 145, y + 39, 0xB8141426);
            g.drawString(mc.font, selected.getName(), x, y, VisionMenuTheme.WHITE);
            if (ENABLED[2]) {
                float hp = Math.max(0, selected.getHealth());
                float max = Math.max(1, selected.getMaxHealth());
                g.fill(x, y + 13, x + 138, y + 19, 0xFF423A53);
                g.fill(x, y + 13, x + (int)(138 * Math.min(1, hp / max)), y + 19, 0xFF87E8BE);
                g.drawString(mc.font, Math.round(hp * 10f) / 10f + " / " +
                    Math.round(max * 10f) / 10f + " HP", x, y + 22, 0xFFADDFD2);
            }
            if (ENABLED[3]) {
                double meters = Math.sqrt(selected.distanceToSqr(mc.player));
                g.drawString(mc.font, "Range: " + Math.round(meters * 10.0) / 10.0 + "m",
                    x + 79, y + 22, 0xFFFFCD8A);
            }
        }
        if (ENABLED[4] && nearestTnt >= 0) {
            Entity e = mc.level.getEntity(nearestTnt);
            if (e != null && e.isAlive()) {
                double dist = Math.sqrt(e.distanceToSqr(mc.player));
                String label = "TNT: " + Math.round(dist * 10.0) / 10.0 + "m";
                if (e instanceof PrimedTnt primed) {
                    label += "  " + Math.max(0, primed.getFuse() / 20.0f) + "s";
                }
                g.drawCenteredString(mc.font, label, w / 2, 26,
                    e instanceof PrimedTnt primed && primed.getFuse() < 20 ? 0xFFFF5B67 : 0xFFFFA177);
            }
        }
        if (ENABLED[5] && nearestArrow >= 0) {
            Entity e = mc.level.getEntity(nearestArrow);
            if (e != null && e.isAlive()) {
                // Project the real arrow position into the GUI. The text stays
                // above the arrow as the camera or Free Look orientation moves.
                // One cached nearest arrow is rendered; no per-frame entity scan.
                var camera = mc.gameRenderer.getMainCamera();
                if (camera.isInitialized()) {
                    Vec3 forward = Vec3.directionFromRotation(camera.xRot(), camera.yRot());
                    Vec3 right = forward.cross(new Vec3(0, 1, 0));
                    if (right.lengthSqr() > 0.00001) {
                        right = right.normalize();
                        Vec3 up = right.cross(forward).normalize();
                        Vec3 toward = e.position().add(0, e.getBbHeight() + 0.20, 0)
                            .subtract(camera.position());
                        double depth = toward.dot(forward);
                        if (depth > .3 && depth < 32) {
                            double factor = h / (2.0 * Math.tan(Math.toRadians(
                                mc.options.fov().get() * .5)));
                            int sx = (int) Math.round(w / 2.0 + toward.dot(right) * factor / depth);
                            int sy = (int) Math.round(h / 2.0 - toward.dot(up) * factor / depth);
                            String label = e.getName().getString();
                            int width = mc.font.width(label);
                            if (sx > width / 2 + 5 && sx < w - width / 2 - 5 &&
                                sy > 20 && sy < h - 25) {
                                g.fill(sx - width / 2 - 4, sy - 14,
                                    sx + width / 2 + 4, sy,
                                    0xB5171529);
                                g.drawCenteredString(mc.font, label, sx, sy - 11, 0xFFBFC4FF);
                            }
                        }
                    }
                }
            }
        }
        if (hitPulse > 0 && (ENABLED[6] || ENABLED[7])) {
            int x = w / 2, y = h / 2, r = 5 + (8 - hitPulse);
            if (ENABLED[6]) {
                int color = (VisionTools2.hitColor() & 0x00FFFFFF) | 0xD0000000;
                g.fill(x - r, y - r, x + r, y - r + 1, color);
                g.fill(x - r, y + r - 1, x + r, y + r, color);
                g.fill(x - r, y - r, x - r + 1, y + r, color);
                g.fill(x + r - 1, y - r, x + r, y + r, color);
            }
            if (ENABLED[7]) {
                int alpha = 12 + hitPulse * 7;
                g.fill(0, 0, w, 3, (alpha << 24) | 0x00C3283F);
                g.fill(0, h - 3, w, h, (alpha << 24) | 0x00C3283F);
            }
        }
    }

    private static void load() {
        if (!Files.isRegularFile(CONFIG)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG)) {
            p.load(in);
            for (int i = 0; i < ENABLED.length; i++)
                ENABLED[i] = Boolean.parseBoolean(p.getProperty("feature." + i, "false"));
        } catch (Exception ex) {
            System.err.println("[VisionMod] FeaturePack settings read failed: " + ex);
        }
    }
    private static void save() {
        Properties p = new Properties();
        for (int i = 0; i < ENABLED.length; i++)
            p.setProperty("feature." + i, Boolean.toString(ENABLED[i]));
        try {
            Files.createDirectories(CONFIG.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG)) {
                p.store(out, "Vision Mod 2.0: independent HUD features (all optional)");
            }
        } catch (Exception ex) {
            System.err.println("[VisionMod] FeaturePack settings save failed: " + ex);
        }
    }
}
