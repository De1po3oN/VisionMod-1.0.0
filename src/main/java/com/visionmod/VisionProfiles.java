package com.visionmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.Desktop;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** Local, shareable JSON profiles. Never executes imported data. */
public final class VisionProfiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z0-9_-]{1,40}");
    private static final Path DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve("visionmod/profiles");

    private VisionProfiles() {}

    public static Path directory() { return DIRECTORY; }

    private static Path path(String name) {
        if (name == null || !VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Use 1-40 English letters, digits, _ or -");
        }
        return DIRECTORY.resolve(name + ".json");
    }

    public static List<String> list() {
        List<String> names = new ArrayList<>();
        try {
            Files.createDirectories(DIRECTORY);
            try (DirectoryStream<Path> files = Files.newDirectoryStream(DIRECTORY, "*.json")) {
                for (Path file : files) {
                    if (!Files.isRegularFile(file) || Files.isSymbolicLink(file)) continue;
                    String name = file.getFileName().toString();
                    name = name.substring(0, name.length() - 5);
                    if (VALID_NAME.matcher(name).matches()) names.add(name);
                }
            }
        } catch (Exception ignored) { }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static void save(String name) throws Exception {
        Files.createDirectories(DIRECTORY);
        Path target = path(name);
        if (Files.isSymbolicLink(target)) throw new IllegalArgumentException("Symlinks are not supported");
        JsonObject data = new JsonObject();
        data.addProperty("format", 1);
        data.addProperty("fullBright", VisionModClient.fullBright);
        data.addProperty("oreHighlight", VisionModClient.oreHighlight);
        data.addProperty("clearWater", VisionModClient.clearWater);
        data.addProperty("clearLava", VisionModClient.clearLava);
        data.addProperty("playerHearts", VisionModClient.playerHearts);
        data.addProperty("particles", VisionModClient.particles);
        data.addProperty("hideBlockParticles", VisionModClient.hideBlockParticles);
        data.addProperty("hideSprintParticles", VisionModClient.hideSprintParticles);
        data.addProperty("hideEnderChestParticles", VisionModClient.hideEnderChestParticles);
        data.addProperty("hideAllParticles", VisionModClient.hideAllParticles);
        data.addProperty("freeLook", FreeLook.enabled);
        data.addProperty("cps", VisionExtras.cps);
        data.addProperty("armorHud", VisionExtras.armorHud);
        data.addProperty("hungerHud", VisionExtras.hungerHud);
        data.addProperty("customCrosshair", VisionExtras.crosshair);
        data.addProperty("zoom", VisionExtras.zoom);
        data.addProperty("fastScroll", VisionExtras.fastScroll);
        data.addProperty("targetOutline", VisionExtras.targetOutline);
        data.addProperty("ambientSnow", VisionExtras.snow);
        data.addProperty("ambientHearts", VisionExtras.hearts);
        data.addProperty("shulkerHud", VisionExtras.shulkerHud);
        data.addProperty("shulkerTooltip", VisionExtras.shulkerTooltip);
        data.addProperty("performanceAdaptive", VisionPerformance.adaptive);
        data.addProperty("performanceBusyThreshold", VisionPerformance.busyPlayerThreshold);
        data.addProperty("performanceLimitAmbientBusy", VisionPerformance.reduceAmbientOnBusySpawn);
        com.google.gson.JsonArray hotkeys = new com.google.gson.JsonArray();
        for (int key : VisionHotkeys.snapshot()) hotkeys.add(key);
        data.add("hotkeys", hotkeys);
        data.addProperty("arrowHitSound", VisionExtras.arrowHitSound);
        data.addProperty("smallSword", VisionExtras.smallSword);
        data.addProperty("smallShield", VisionExtras.smallShield);
        data.addProperty("smallTotem", VisionExtras.smallTotem);
        data.addProperty("lowFire", VisionExtras.lowFire);
        data.addProperty("crosshairPixels", VisionExtras.pixels());
        data.addProperty("crosshairSize", VisionExtras.crosshairSize);
        data.addProperty("crosshairGrid", VisionExtras.crosshairGrid);
        data.addProperty("themePalette", VisionMenuTheme.palette());
        data.addProperty("renderBoost", VisionRenderBoost.enabled);
        data.addProperty("renderBoostCap", VisionRenderBoost.chunkCap);
        data.addProperty("renderBoostTargetFps", VisionRenderBoost.targetFps);
        data.addProperty("renderBoostAdaptive", VisionRenderBoost.adaptiveChunks);
        data.addProperty("performanceLimitHudBusy", VisionPerformance.limitHudOnBusySpawn);
        data.addProperty("totemAnimation", VisionAdvanced.smallTotemAnimation);
        data.addProperty("totemAnimationPercent", VisionAdvanced.totemAnimationPercent);
        data.addProperty("shulkerGrid", VisionAdvanced.shulkerGrid);
        data.addProperty("crosshairColor", String.format("#%06X", VisionExtras.crosshairColor & 0xFFFFFF));
        data.addProperty("outlineColor", String.format("#%06X", VisionExtras.outlineColor & 0xFFFFFF));
        data.addProperty("zoomFactor", VisionExtras.zoomFactor);
        // Optional v0.7.0 settings; legacy v0.6.0 profile files stay loadable.
        data.addProperty("performanceRadius", VisionPerformance.oreRadius);
        data.addProperty("performanceBudget", VisionPerformance.checksPerTick);
        data.addProperty("performanceOutlines", VisionPerformance.outlineLimit);
        com.google.gson.JsonArray colors = new com.google.gson.JsonArray();
        com.google.gson.JsonArray ores = new com.google.gson.JsonArray();
        for (int i = 0; i < OreColors.NAMES.length; i++) {
            colors.add(String.format("#%06X", OreColors.getColor(i) & 0xFFFFFF));
            ores.add(OreSelection.enabled(i));
        }
        data.add("oreColors", colors);
        data.add("oreEnabled", ores);
        String output = GSON.toJson(data);
        Path temporary = Files.createTempFile(DIRECTORY, "profile-", ".tmp");
        try {
            Files.writeString(temporary, output, StandardCharsets.UTF_8);
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    public static void load(String name) throws Exception {
        Path file = path(name);
        if (!Files.isRegularFile(file) || Files.isSymbolicLink(file)) throw new IllegalArgumentException("Profile not found");
        if (Files.size(file) > 16384L) throw new IllegalArgumentException("Profile is too large");
        JsonObject data = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        if (!data.has("format") || data.get("format").getAsInt() != 1) throw new IllegalArgumentException("Unsupported profile format");
        // Validate every field before changing any mod settings.
        boolean[] switches = new boolean[] {
            flag(data, "fullBright"), flag(data, "oreHighlight"), flag(data, "clearWater"),
            flag(data, "clearLava"), flag(data, "playerHearts"), flag(data, "particles"),
            flag(data, "hideBlockParticles"), flag(data, "hideSprintParticles"),
            flag(data, "hideEnderChestParticles"), flag(data, "hideAllParticles"), flag(data, "freeLook")
        };
        var colors = data.getAsJsonArray("oreColors");
        var ores = data.getAsJsonArray("oreEnabled");
        if (colors == null || ores == null || colors.size() != OreColors.NAMES.length || ores.size() != OreColors.NAMES.length)
            throw new IllegalArgumentException("Ore settings are missing");
        int[] validatedColors = new int[colors.size()];
        boolean[] validatedOres = new boolean[ores.size()];
        for (int i = 0; i < colors.size(); i++) {
            String hex = colors.get(i).getAsString();
            if (!hex.matches("#[0-9A-Fa-f]{6}")) throw new IllegalArgumentException("Invalid ore color");
            validatedColors[i] = 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
            if (!ores.get(i).isJsonPrimitive() || !ores.get(i).getAsJsonPrimitive().isBoolean())
                throw new IllegalArgumentException("Invalid ore toggle");
            validatedOres[i] = ores.get(i).getAsBoolean();
        }
        // Accept optional 0.7.0 numeric fields only if they match UI-supported values.
        int radius = optionalChoice(data, "performanceRadius", VisionPerformance.oreRadius, 4, 6, 8, 12, 16);
        int budget = optionalChoice(data, "performanceBudget", VisionPerformance.checksPerTick, 40, 80, 160, 320);
        int outlines = optionalChoice(data, "performanceOutlines", VisionPerformance.outlineLimit, 32, 48, 96, 160, 240);
        // Profiles made by older releases omit extras and remain supported.
        boolean exCps = optionalFlag(data, "cps", VisionExtras.cps);
        boolean exArmor = optionalFlag(data, "armorHud", VisionExtras.armorHud);
        boolean exFood = optionalFlag(data, "hungerHud", VisionExtras.hungerHud);
        boolean exReticle = optionalFlag(data, "customCrosshair", VisionExtras.crosshair);
        boolean exZoom = optionalFlag(data, "zoom", VisionExtras.zoom);
        boolean exFast = optionalFlag(data, "fastScroll", VisionExtras.fastScroll);
        boolean exOutline = optionalFlag(data, "targetOutline", VisionExtras.targetOutline);
        boolean exSnow = optionalFlag(data, "ambientSnow", VisionExtras.snow);
        boolean exHearts = optionalFlag(data, "ambientHearts", VisionExtras.hearts);
        boolean exShulker = optionalFlag(data, "shulkerHud", VisionExtras.shulkerHud);
        boolean exShulkerTooltip = optionalFlag(data, "shulkerTooltip", VisionExtras.shulkerTooltip);
        boolean perfAdaptive = optionalFlag(data, "performanceAdaptive", VisionPerformance.adaptive);
        int busyThreshold = optionalChoice(data, "performanceBusyThreshold", VisionPerformance.busyPlayerThreshold, 8,12,16,24,32);
        boolean perfAmbient = optionalFlag(data, "performanceLimitAmbientBusy", VisionPerformance.reduceAmbientOnBusySpawn);
        boolean limitHud = optionalFlag(data, "performanceLimitHudBusy", VisionPerformance.limitHudOnBusySpawn);
        int[] profileHotkeys = null;
        if (data.has("hotkeys")) {
            com.google.gson.JsonArray keys = data.getAsJsonArray("hotkeys");
            if (keys == null || keys.size() != VisionHotkeys.count() &&
                keys.size() != VisionHotkeys.PREVIOUS_COUNT && keys.size() != VisionHotkeys.LEGACY_COUNT && keys.size() != VisionHotkeys.PREVIOUS_53_COUNT)
                throw new IllegalArgumentException("Invalid hotkeys list");
            profileHotkeys = new int[VisionHotkeys.count()];
            java.util.HashSet<Integer> seenKeys = new java.util.HashSet<>();
            for (int i = 0; i < keys.size(); i++) {
                if (!keys.get(i).isJsonPrimitive() || !keys.get(i).getAsJsonPrimitive().isNumber())
                    throw new IllegalArgumentException("Invalid hotkey");
                int key = keys.get(i).getAsInt();
                if (!VisionHotkeys.valid(key) || key != 0 && !seenKeys.add(key))
                    throw new IllegalArgumentException("Invalid or duplicate hotkey");
                profileHotkeys[i] = key;
            }
        }
        boolean exArrowHit = optionalFlag(data, "arrowHitSound", VisionExtras.arrowHitSound);
        boolean exSword = optionalFlag(data, "smallSword", VisionExtras.smallSword);
        boolean exShield = optionalFlag(data, "smallShield", VisionExtras.smallShield);
        boolean exTotem = optionalFlag(data, "smallTotem", VisionExtras.smallTotem);
        boolean exFire = optionalFlag(data, "lowFire", VisionExtras.lowFire);
        String crosshairPixels = data.has("crosshairPixels") ? data.get("crosshairPixels").getAsString() : VisionExtras.pixels();
        if (!VisionExtras.validPixelString(crosshairPixels))
            throw new IllegalArgumentException("Invalid crosshair drawing");
        int crosshairSize = optionalChoice(data,"crosshairSize",VisionExtras.crosshairSize,1,2,3,4);
        int themePalette = optionalChoice(data,"themePalette",VisionMenuTheme.palette(),0,1,2,3,4,5);
        boolean renderBoost = optionalFlag(data,"renderBoost",VisionRenderBoost.enabled);
        int renderBoostCap = optionalChoice(data,"renderBoostCap",VisionRenderBoost.chunkCap,4,6,8);
        int boostTarget = optionalChoice(data,"renderBoostTargetFps",VisionRenderBoost.targetFps,40,50,60,75);
        boolean adaptiveChunks = optionalFlag(data,"renderBoostAdaptive",VisionRenderBoost.adaptiveChunks);
        boolean totemAnimation = optionalFlag(data,"totemAnimation",VisionAdvanced.smallTotemAnimation);
        int totemAnimationPercent = optionalChoice(data,"totemAnimationPercent",VisionAdvanced.totemAnimationPercent,50,75,100);
        boolean shulkerGrid = optionalFlag(data,"shulkerGrid",VisionAdvanced.shulkerGrid);
        int exCrossColor = optionalHex(data, "crosshairColor", VisionExtras.crosshairColor);
        int exOutlineColor = optionalHex(data, "outlineColor", VisionExtras.outlineColor);
        float exZoomFactor = VisionExtras.zoomFactor;
        if (data.has("zoomFactor")) {
            exZoomFactor = data.get("zoomFactor").getAsFloat();
            if (exZoomFactor != .2f && exZoomFactor != .35f && exZoomFactor != .5f)
                throw new IllegalArgumentException("Unsupported zoom factor");
        }
        VisionModClient.fullBright = switches[0];
        VisionModClient.oreHighlight = switches[1];
        VisionModClient.clearWater = switches[2];
        VisionModClient.clearLava = switches[3];
        VisionModClient.playerHearts = switches[4];
        VisionModClient.particles = switches[5];
        VisionModClient.hideBlockParticles = switches[6];
        VisionModClient.hideSprintParticles = switches[7];
        VisionModClient.hideEnderChestParticles = switches[8];
        VisionModClient.hideAllParticles = switches[9];
        FreeLook.enabled = switches[10];
        VisionPerformance.oreRadius = radius;
        VisionPerformance.checksPerTick = budget;
        VisionPerformance.outlineLimit = outlines;
        VisionExtras.cps = exCps;
        VisionExtras.armorHud = exArmor;
        VisionExtras.hungerHud = exFood;
        VisionExtras.crosshair = exReticle;
        VisionExtras.zoom = exZoom;
        VisionExtras.fastScroll = exFast;
        VisionExtras.targetOutline = exOutline;
        VisionExtras.snow = exSnow;
        VisionExtras.hearts = exHearts;
        VisionExtras.shulkerHud = exShulker;
        VisionExtras.shulkerTooltip = exShulkerTooltip;
        VisionPerformance.adaptive = perfAdaptive;
        VisionPerformance.busyPlayerThreshold = busyThreshold;
        VisionPerformance.reduceAmbientOnBusySpawn = perfAmbient;
        VisionPerformance.limitHudOnBusySpawn = limitHud;
        VisionExtras.arrowHitSound = exArrowHit;
        VisionExtras.smallSword = exSword;
        VisionExtras.smallShield = exShield;
        VisionExtras.smallTotem = exTotem;
        VisionExtras.lowFire = exFire;
        VisionExtras.setPixels(crosshairPixels);
        VisionExtras.setCrosshairSize(crosshairSize);
        VisionMenuTheme.setPalette(themePalette);
        VisionRenderBoost.enabled = renderBoost;
        VisionRenderBoost.chunkCap = renderBoostCap;
        VisionRenderBoost.targetFps = boostTarget;
        VisionRenderBoost.adaptiveChunks = adaptiveChunks;
        VisionRenderBoost.save();
        VisionAdvanced.smallTotemAnimation = totemAnimation;
        VisionAdvanced.totemAnimationPercent = totemAnimationPercent;
        VisionAdvanced.shulkerGrid = shulkerGrid;
        VisionAdvanced.save();
        VisionExtras.crosshairColor = exCrossColor;
        VisionExtras.outlineColor = exOutlineColor;
        VisionExtras.zoomFactor = exZoomFactor;
        VisionExtras.save();
        VisionPerformance.custom();
        if (profileHotkeys != null) VisionHotkeys.replaceAll(profileHotkeys);
        for (int i = 0; i < validatedColors.length; i++) {
            OreColors.setColor(i, validatedColors[i]);
            OreSelection.setEnabled(i, validatedOres[i]);
        }
    }

    private static boolean flag(JsonObject data, String key) {
        if (!data.has(key) || !data.get(key).isJsonPrimitive() || !data.getAsJsonPrimitive(key).isBoolean())
            throw new IllegalArgumentException("Invalid setting: " + key);
        return data.get(key).getAsBoolean();
    }

    private static int optionalChoice(JsonObject data, String key, int defaultValue, int... allowed) {
        if (!data.has(key)) return defaultValue;
        if (!data.get(key).isJsonPrimitive() || !data.get(key).getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Invalid performance setting: " + key);
        int actual = data.get(key).getAsInt();
        for (int value : allowed) if (actual == value) return actual;
        throw new IllegalArgumentException("Unsupported performance setting: " + key);
    }

    private static int optionalHex(JsonObject data, String key, int fallback) {
        if (!data.has(key)) return fallback;
        String color = data.get(key).getAsString();
        if (!color.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Invalid color: " + key);
        return 0xFF000000 | Integer.parseInt(color.substring(1), 16);
    }

    private static boolean optionalFlag(JsonObject data, String key, boolean defaultValue) {
        return data.has(key) ? flag(data, key) : defaultValue;
    }

    public static void delete(String name) throws Exception {
        Path target = path(name);
        if (Files.isSymbolicLink(target)) throw new IllegalArgumentException("Symlinks are not supported");
        Files.deleteIfExists(target);
    }

    public static boolean openFolder() {
        try {
            Files.createDirectories(DIRECTORY);
            if (!Desktop.isDesktopSupported()) return false;
            Desktop.getDesktop().open(DIRECTORY.toFile());
            return true;
        } catch (Exception ignored) { return false; }
    }
}
