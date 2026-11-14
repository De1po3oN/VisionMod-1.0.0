package com.visionmod;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;
import java.util.Random;

/** Independent, low-overhead client enhancements. No server plugin required. */
public final class VisionExtras {
    public static boolean cps = false;
    public static boolean armorHud = false;
    public static boolean hungerHud = false;
    public static boolean crosshair = false;
    public static boolean zoom = true;
    public static boolean fastScroll = false;
    public static boolean targetOutline = false;
    public static boolean snow = false;
    public static boolean hearts = false;
    /** 0 = drifting around the player, 1 = soft halo in the view direction. */
    public static int ambientStyle = 0;
    /** 1 = subtle, 2 = balanced, 3 = vivid. Small bounded spawn budget. */
    public static int ambientDensity = 2;
    public static boolean shulkerHud = false;
    public static boolean shulkerTooltip = false;
    public static boolean arrowHitSound = false;
    public static boolean smallSword = false;
    public static boolean smallShield = false;
    public static boolean smallTotem = false;
    public static boolean lowFire = false;
    public static volatile boolean zoomHeld = false;
    public static int crosshairColor = 0xFF48E7F6;
    public static int outlineColor = 0xFFFF78CB;
    public static float zoomFactor = .35f;
    // 16/32/64 cell grids. Runs are cached to avoid thousands of GPU rectangles per frame.
    public static int crosshairGrid = 16;
    public static int crosshairSize = 2;
    private static boolean[] CUSTOM_PIXELS = defaultPixels(16);
    private static int[] runs = buildRuns();

    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
        .resolve("visionmod/extras.properties");
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final Deque<Long> CLICKS = new ArrayDeque<>();
    private static final Random RANDOM = new Random();
    private static int tick;
    private static boolean spawningOwnAmbient;
    public static boolean isSpawningOwnAmbient() { return spawningOwnAmbient; }

    private VisionExtras() { }

    private static boolean[] defaultPixels(int grid) {
        boolean[] data = new boolean[grid * grid];
        int center = grid / 2;
        for (int d = 1; d <= 3; d++) {
            data[center * grid + center - d] = true;
            data[center * grid + center + d] = true;
            data[(center - d) * grid + center] = true;
            data[(center + d) * grid + center] = true;
        }
        data[center * grid + center] = true;
        return data;
    }

    public static boolean pixel(int x, int y) {
        return x >= 0 && x < crosshairGrid && y >= 0 && y < crosshairGrid && CUSTOM_PIXELS[y * crosshairGrid + x];
    }
    public static void togglePixel(int x, int y) {
        if (x < 0 || x >= crosshairGrid || y < 0 || y >= crosshairGrid) return;
        CUSTOM_PIXELS[y * crosshairGrid + x] = !CUSTOM_PIXELS[y * crosshairGrid + x];
        runs = buildRuns();
        save();
    }
    public static void resetPixels() {
        CUSTOM_PIXELS = defaultPixels(crosshairGrid);
        runs = buildRuns();
        save();
    }
    public static void setCrosshairGrid(int grid) {
        if (grid != 16 && grid != 32 && grid != 64 || grid == crosshairGrid) return;
        boolean[] resized = new boolean[grid * grid];
        // Preserve the drawing's angular size and shape on a larger grid.
        int old = crosshairGrid;
        for (int row = 0; row < old; row++) for (int col = 0; col < old; col++) {
            if (!CUSTOM_PIXELS[row * old + col]) continue;
            int xx = Math.round((col - old / 2f) * grid / old + grid / 2f);
            int yy = Math.round((row - old / 2f) * grid / old + grid / 2f);
            if (xx >= 0 && xx < grid && yy >= 0 && yy < grid) resized[yy * grid + xx] = true;
        }
        crosshairGrid = grid;
        CUSTOM_PIXELS = resized;
        runs = buildRuns();
        save();
    }
    public static void setCrosshairSize(int size) {
        crosshairSize = Math.max(1, Math.min(4, size));
        save();
    }
    public static String pixels() {
        StringBuilder result = new StringBuilder(CUSTOM_PIXELS.length);
        for (boolean value : CUSTOM_PIXELS) result.append(value ? '1' : '0');
        return result.toString();
    }
    public static boolean validPixelString(String encoded) {
        if (encoded == null) return false;
        int length = encoded.length();
        if (length != 81 && length != 256 && length != 1024 && length != 4096) return false;
        for (int i=0; i<length; i++) if (encoded.charAt(i) != '0' && encoded.charAt(i) != '1') return false;
        return true;
    }
    public static void setPixels(String encoded) {
        applyPixels(encoded);
        save();
    }
    private static void applyPixels(String encoded) {
        if (!validPixelString(encoded)) throw new IllegalArgumentException("Invalid crosshair pixel grid");
        int count = encoded.length();
        if (count == 81) { // Old 9x9 user configs remain importable.
            crosshairGrid = 16;
            CUSTOM_PIXELS = new boolean[256];
            for (int y=0; y<9; y++) for (int x=0; x<9; x++)
                CUSTOM_PIXELS[(y+4)*16 + x+4] = encoded.charAt(y*9+x) == '1';
        } else {
            crosshairGrid = (int)Math.sqrt(count);
            CUSTOM_PIXELS = new boolean[count];
            for (int i=0; i<count; i++) CUSTOM_PIXELS[i] = encoded.charAt(i) == '1';
        }
        runs = buildRuns();
    }

    /** Each triplet is x, y, horizontal-run length. Rebuilt only after editing. */
    private static int[] buildRuns() {
        java.util.ArrayList<Integer> list = new java.util.ArrayList<>();
        for (int y=0; y<crosshairGrid; y++) {
            int x = 0;
            while (x < crosshairGrid) {
                if (!CUSTOM_PIXELS[y*crosshairGrid+x]) { x++; continue; }
                int start = x;
                do { x++; } while (x < crosshairGrid && CUSTOM_PIXELS[y*crosshairGrid+x]);
                list.add(start); list.add(y); list.add(x-start);
            }
        }
        int[] result = new int[list.size()];
        for (int i=0; i<list.size(); i++) result[i] = list.get(i);
        return result;
    }

    public static void init() {
        load();
        VisionShulkerTooltip.init();
        // Replace only the vanilla crosshair; default behavior is retained when disabled.
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, tracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!VisionModClient.enabled || !crosshair || mc.player == null || mc.screen != null) {
                original.render(graphics, tracker);
            } else {
                drawCrosshair(graphics);
            }
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("visionmod", "extra_hud"), (graphics, tracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!VisionModClient.enabled || mc.player == null || mc.level == null || mc.screen != null) return;
            int y = 8;
            if (cps) {
                graphics.drawString(mc.font, "CPS: " + clickRate(), 8, y, 0xFFF2F7FF);
                y += 13;
            }
            if (hungerHud) {
                var food = mc.player.getFoodData();
                graphics.drawString(mc.font, "Food " + food.getFoodLevel() + "/20  Sat "
                    + Math.round(food.getSaturationLevel() * 10f) / 10f, 8, y, 0xFFFFD166);
                y += 13;
            }
            if (armorHud) {
                int pieces = 0, worn = 0, total = 0;
                for (EquipmentSlot slot : ARMOR_SLOTS) {
                    ItemStack stack = mc.player.getItemBySlot(slot);
                    if (!stack.isEmpty()) {
                        pieces++;
                        if (stack.isDamageableItem()) {
                            worn += Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
                            total += stack.getMaxDamage();
                        }
                    }
                }
                String label = "Armor " + pieces + "/4";
                if (total > 0) label += "  Dur " + Math.round(100f * worn / total) + "%";
                graphics.drawString(mc.font, label, 8, y, 0xFF65F0B2);
                y += 13;
            }
            if (shulkerHud) {
                ItemStack held = mc.player.getMainHandItem();
                if (held.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock) {
                    ItemContainerContents stored = held.get(DataComponents.CONTAINER);
                    int distinct = 0, totalItems = 0;
                    if (stored != null) {
                        for (ItemStack content : stored.nonEmptyItems()) {
                            distinct++;
                            totalItems += content.getCount();
                        }
                    }
                    graphics.drawString(mc.font, "Shulker: " + distinct + " stacks / " + totalItems + " items",
                        8, y, 0xFFAC79FF);
                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.level == null || mc.player == null) { zoomHeld = false; tick = 0; return; }
            if (!VisionModClient.enabled || mc.screen != null || (!snow && !hearts)) {
                tick = 0;
                return;
            }
            // Camera/world-space ambience. Density never bypasses the performance budget.
            // Keep a visible but bounded population. High density is capped
            // at ~20 sprites/sec; this does not scale with player/entity count.
            int interval = Math.max(3, VisionPerformance.ambientInterval() * 2 / ambientDensity);
            if (++tick < interval) return;
            tick = 0;

            var camera = mc.gameRenderer.getMainCamera();
            Vec3 eye = camera.isInitialized() ? camera.position() : mc.player.getEyePosition();
            Vec3 facing = camera.isInitialized()
                ? Vec3.directionFromRotation(camera.xRot(), camera.yRot())
                : mc.player.getLookAngle();
            Vec3 right = new Vec3(-facing.z, 0, facing.x);
            right = right.lengthSqr() < 0.001 ? new Vec3(1, 0, 0) : right.normalize();
            Vec3 up = right.cross(facing).normalize();
            boolean worldRing = ambientStyle == 0 &&
                mc.options.getCameraType() != CameraType.FIRST_PERSON;
            // A full 360-degree ring was almost invisible in first person:
            // most particles appeared behind the camera. Keep first-person
            // particles in the visible forward hemisphere but in world space.
            Vec3 origin = worldRing
                ? mc.player.position().add(0, 1.1, 0)
                : eye.add(facing.scale(2.8));
            int count = ambientDensity;
            spawningOwnAmbient = true;
            try {
                for (int i = 0; i < count; i++) {
                    Vec3 point;
                    if (worldRing) {
                        // Stable world particles drift beside the player, not glued to the screen.
                        double angle = RANDOM.nextDouble() * Math.PI * 2;
                        double distance = 1.1 + RANDOM.nextDouble() * 3.1;
                        point = origin.add(Math.cos(angle) * distance,
                            -0.75 + RANDOM.nextDouble() * 2.7,
                            Math.sin(angle) * distance);
                    } else {
                        // An optional, spacious view-facing constellation.
                        double angle = RANDOM.nextDouble() * Math.PI * 2;
                        double radius = 0.55 + RANDOM.nextDouble() * 1.6;
                        point = origin.add(right.scale(Math.cos(angle) * radius))
                            .add(up.scale(Math.sin(angle) * radius))
                            .add(facing.scale((RANDOM.nextDouble() - 0.5) * 1.2));
                    }
                    double driftX = (RANDOM.nextDouble() - 0.5) * 0.008;
                    double driftZ = (RANDOM.nextDouble() - 0.5) * 0.008;
                    // Explicitly enabled FX stay visible with MAX FPS / MINIMAL particles.
                    if (hearts) mc.level.addAlwaysVisibleParticle(ParticleTypes.HEART,
                        point.x, point.y, point.z, driftX, 0.015, driftZ);
                    if (snow && (i == 0 || ambientDensity >= 3)) {
                        Vec3 snowPoint = point.add(0.25, 0.5, -0.2);
                        mc.level.addAlwaysVisibleParticle(ParticleTypes.SNOWFLAKE,
                            snowPoint.x, snowPoint.y, snowPoint.z, driftX, -0.008, driftZ);
                    }
                }
            } finally {
                spawningOwnAmbient = false;
            }
        });
    }

    private static void drawCrosshair(GuiGraphics g) {
        int cx = g.guiWidth()/2, cy = g.guiHeight()/2;
        int step = crosshairSize;
        int left = cx - (crosshairGrid/2)*step;
        int top = cy - (crosshairGrid/2)*step;
        int[] snapshot = runs;
        for (int i=0; i<snapshot.length; i+=3) {
            int x = left+snapshot[i]*step, y = top+snapshot[i+1]*step;
            g.fill(x, y, x+snapshot[i+2]*step, y+step, crosshairColor);
        }
    }

    public static void clicked() {
        if (!VisionModClient.enabled || !cps) return;
        synchronized (CLICKS) {
            long now = System.currentTimeMillis();
            CLICKS.addLast(now);
            while (CLICKS.size() > 100) CLICKS.removeFirst();
        }
    }

    private static int clickRate() {
        synchronized (CLICKS) {
            long cutoff = System.currentTimeMillis() - 1000;
            while (!CLICKS.isEmpty() && CLICKS.peekFirst() < cutoff) CLICKS.removeFirst();
            return CLICKS.size();
        }
    }

    public static void toggle(int index) {
        switch (index) {
            case 0 -> cps = !cps;
            case 1 -> armorHud = !armorHud;
            case 2 -> hungerHud = !hungerHud;
            case 3 -> crosshair = !crosshair;
            case 4 -> zoom = !zoom;
            case 5 -> fastScroll = !fastScroll;
            case 6 -> targetOutline = !targetOutline;
            case 7 -> snow = !snow;
            case 8 -> hearts = !hearts;
            case 9 -> shulkerHud = !shulkerHud;
            case 10 -> arrowHitSound = !arrowHitSound;
            case 11 -> smallSword = !smallSword;
            case 12 -> smallShield = !smallShield;
            case 13 -> smallTotem = !smallTotem;
            case 14 -> lowFire = !lowFire;
            case 15 -> shulkerTooltip = !shulkerTooltip;
            default -> { return; }
        }
        save();
        VisionPerformance.custom();
    }

    public static boolean enabled(int index) {
        return switch (index) {
            case 0 -> cps;
            case 1 -> armorHud;
            case 2 -> hungerHud;
            case 3 -> crosshair;
            case 4 -> zoom;
            case 5 -> fastScroll;
            case 6 -> targetOutline;
            case 7 -> snow;
            case 8 -> hearts;
            case 9 -> shulkerHud;
            case 10 -> arrowHitSound;
            case 11 -> smallSword;
            case 12 -> smallShield;
            case 13 -> smallTotem;
            case 14 -> lowFire;
            case 15 -> shulkerTooltip;
            default -> false;
        };
    }

    public static void cycleCrosshairColor() {
        crosshairColor = nextColor(crosshairColor);
        save();
    }
    public static void cycleOutlineColor() {
        outlineColor = nextColor(outlineColor);
        save();
    }
    private static int nextColor(int current) {
        int[] choices = {0xFF48E7F6, 0xFFFF78CB, 0xFFFFD166, 0xFF65F0B2, 0xFFFFFFFF, 0xFFAC79FF};
        for (int i = 0; i < choices.length; i++) if (choices[i] == current) return choices[(i+1)%choices.length];
        return choices[0];
    }
    public static void cycleAmbientStyle() {
        ambientStyle = 1 - ambientStyle;
        save();
    }
    public static void cycleAmbientDensity() {
        ambientDensity = ambientDensity % 3 + 1;
        save();
    }
    public static String ambientStyleLabel() { return ambientStyle == 0 ? "WORLD" : "CAMERA"; }
    public static String ambientDensityLabel() {
        return switch (ambientDensity) { case 1 -> "LOW"; case 3 -> "HIGH"; default -> "MEDIUM"; };
    }

    public static void cycleZoom() {
        zoomFactor = zoomFactor == .35f ? .5f : zoomFactor == .5f ? .2f : .35f;
        save();
    }

    public static void load() {
        if (!Files.isRegularFile(FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
            cps = value(p, "cps", cps); armorHud = value(p, "armorHud", armorHud);
            hungerHud = value(p, "hungerHud", hungerHud); crosshair = value(p, "crosshair", crosshair);
            zoom = value(p, "zoom", zoom); fastScroll = value(p, "fastScroll", fastScroll);
            targetOutline = value(p, "targetOutline", targetOutline); snow = value(p, "snow", snow);
            hearts = value(p, "hearts", hearts); shulkerHud = value(p, "shulkerHud", shulkerHud);
            ambientStyle = Math.max(0, Math.min(1, Integer.parseInt(p.getProperty("ambientStyle", "0"))));
            ambientDensity = Math.max(1, Math.min(3, Integer.parseInt(p.getProperty("ambientDensity", "2"))));
            shulkerTooltip = value(p, "shulkerTooltip", shulkerTooltip);
            arrowHitSound = value(p, "arrowHitSound", arrowHitSound);
            smallSword = value(p, "smallSword", smallSword);
            smallShield = value(p, "smallShield", smallShield);
            smallTotem = value(p, "smallTotem", smallTotem);
            lowFire = value(p, "lowFire", lowFire);
            FreeLook.smooth = value(p, "freeLookSmooth", FreeLook.smooth);
            FreeLook.enabled = value(p, "freeLookEnabled", FreeLook.enabled);
            String savedPattern = p.getProperty("crosshairPixels");
            if (validPixelString(savedPattern)) applyPixels(savedPattern);
            crosshairSize = Math.max(1, Math.min(4,
                Integer.parseInt(p.getProperty("crosshairSize", "2"))));
            crosshairColor = color(p, "crosshairColor", crosshairColor);
            outlineColor = color(p, "outlineColor", outlineColor);
            float z = Float.parseFloat(p.getProperty("zoomFactor", Float.toString(zoomFactor)));
            if (z == .2f || z == .35f || z == .5f) zoomFactor = z;
        } catch (Exception e) {
            System.err.println("[VisionMod] Could not load extra settings: " + e);
        }
    }
    private static boolean value(Properties p, String key, boolean def) {
        String value = p.getProperty(key);
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        return def;
    }
    private static int color(Properties p, String key, int def) {
        try { return (int) Long.parseLong(p.getProperty(key), 16) | 0xFF000000; }
        catch (RuntimeException e) { return def; }
    }
    public static void save() {
        Properties p = new Properties();
        p.setProperty("cps", Boolean.toString(cps)); p.setProperty("armorHud", Boolean.toString(armorHud));
        p.setProperty("hungerHud", Boolean.toString(hungerHud)); p.setProperty("crosshair", Boolean.toString(crosshair));
        p.setProperty("zoom", Boolean.toString(zoom)); p.setProperty("fastScroll", Boolean.toString(fastScroll));
        p.setProperty("targetOutline", Boolean.toString(targetOutline)); p.setProperty("snow", Boolean.toString(snow));
        p.setProperty("hearts", Boolean.toString(hearts)); p.setProperty("shulkerHud", Boolean.toString(shulkerHud));
        p.setProperty("ambientStyle", Integer.toString(ambientStyle));
        p.setProperty("ambientDensity", Integer.toString(ambientDensity));
        p.setProperty("freeLookSmooth", Boolean.toString(FreeLook.smooth));
        p.setProperty("freeLookEnabled", Boolean.toString(FreeLook.enabled));
        p.setProperty("shulkerTooltip", Boolean.toString(shulkerTooltip));
        p.setProperty("arrowHitSound", Boolean.toString(arrowHitSound));
        p.setProperty("smallSword", Boolean.toString(smallSword));
        p.setProperty("smallShield", Boolean.toString(smallShield));
        p.setProperty("smallTotem", Boolean.toString(smallTotem));
        p.setProperty("lowFire", Boolean.toString(lowFire));
        p.setProperty("crosshairColor", Integer.toHexString(crosshairColor));
        p.setProperty("crosshairPixels", pixels());
        p.setProperty("crosshairSize", Integer.toString(crosshairSize));
        p.setProperty("crosshairGrid", Integer.toString(crosshairGrid));
        p.setProperty("outlineColor", Integer.toHexString(outlineColor));
        p.setProperty("zoomFactor", Float.toString(zoomFactor));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) { p.store(out, "Vision Mod optional client enhancements"); }
        } catch (Exception e) { System.err.println("[VisionMod] Could not save extra settings: " + e); }
    }
}
