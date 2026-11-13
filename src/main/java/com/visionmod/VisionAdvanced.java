package com.visionmod;

import net.fabricmc.loader.api.FabricLoader;

import java.awt.Desktop;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Optional modules and integrations. Never download or execute third-party code automatically. */
public final class VisionAdvanced {
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("visionmod/advanced.properties");
    public static boolean smallTotemAnimation = false;
    /** 50, 75 or 100 percent. */
    public static int totemAnimationPercent = 75;
    public static boolean shulkerGrid = false;

    private VisionAdvanced() { }

    public static void load() {
        if (!Files.isRegularFile(CONFIG)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG)) {
            p.load(in);
            smallTotemAnimation = Boolean.parseBoolean(p.getProperty("smallTotemAnimation", "false"));
            shulkerGrid = Boolean.parseBoolean(p.getProperty("shulkerGrid", "false"));
            int n = Integer.parseInt(p.getProperty("totemAnimationPercent", "75"));
            if (n == 50 || n == 75 || n == 100) totemAnimationPercent = n;
        } catch (Exception ex) { System.err.println("[VisionMod] Advanced config: " + ex); }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG.getParent());
            Properties p = new Properties();
            p.setProperty("smallTotemAnimation", Boolean.toString(smallTotemAnimation));
            p.setProperty("shulkerGrid", Boolean.toString(shulkerGrid));
            p.setProperty("totemAnimationPercent", Integer.toString(totemAnimationPercent));
            try (OutputStream out = Files.newOutputStream(CONFIG)) {
                p.store(out, "Vision Mod optional advanced effects");
            }
        } catch (Exception ex) { System.err.println("[VisionMod] Save advanced config: " + ex); }
    }

    public static void cycleTotemSize() {
        totemAnimationPercent = totemAnimationPercent == 100 ? 75 : totemAnimationPercent == 75 ? 50 : 100;
        save();
    }

    public static boolean installed(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static boolean openResourcepacksFolder() {
        return openFolder(FabricLoader.getInstance().getGameDir().resolve("resourcepacks"));
    }

    public static boolean openModsFolder() {
        return openFolder(FabricLoader.getInstance().getGameDir().resolve("mods"));
    }

    public static boolean openFolder(Path folder) {
        try {
            Files.createDirectories(folder);
            if (!Desktop.isDesktopSupported()) return false;
            Desktop.getDesktop().open(folder.toFile());
            return true;
        } catch (Exception ex) {
            System.err.println("[VisionMod] Cannot open folder: " + ex);
            return false;
        }
    }

    public static boolean openOfficialPage(String url) {
        try {
            if (!url.startsWith("https://modrinth.com/mod/")) return false;
            if (!Desktop.isDesktopSupported()) return false;
            Desktop.getDesktop().browse(URI.create(url));
            return true;
        } catch (Exception ex) {
            System.err.println("[VisionMod] Cannot open browser: " + ex);
            return false;
        }
    }
}
