package com.visionmod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/**
 * CI-only live-client test of feature-toggle wiring and persisted settings.
 * This catches an entire class of "button exists but changes nothing" bugs.
 * It DOES NOT verify rendered visuals, damage mechanics or FPS.
 */
public final class VisionSelfTest {
    private VisionSelfTest() {}

    public static void run() {
        int passed = 0;
        passed += verify("HUD/COMBAT", VisionFeaturePack.NAMES,
            VisionFeaturePack::enabled, VisionFeaturePack::toggle,
            "features-2.properties", "feature.");
        passed += verify("WORLD FX", VisionWorldEffects.NAMES,
            VisionWorldEffects::enabled, VisionWorldEffects::toggle,
            "world-effects-2.properties", "effect.");
        passed += verify("EXTRA FX", VisionExtendedFeatures.NAMES,
            VisionExtendedFeatures::enabled, VisionExtendedFeatures::toggle,
            "extended-2.properties", "feature.");
        passed += verify("MOTION FX", VisionMotionEffects.NAMES,
            VisionMotionEffects::selected, VisionMotionEffects::toggle,
            "motion-2.properties", "motion.");
        passed += verify("TOOLS", VisionTools2.NAMES,
            VisionTools2::selected, VisionTools2::toggle,
            "tools-2.properties", "tool.");
        String[] extras = {
            "CPS", "Armor HUD", "Food HUD", "Crosshair", "Zoom", "Fast Scroll",
            "Outline", "Snowflakes", "Air Hearts", "Shulker HUD", "Arrow Hit",
            "Small Sword", "Small Shield", "Small Totem", "Low Fire", "Shulker Tooltip"
        };
        String[] extrasKeys = {
            "cps", "armorHud", "hungerHud", "crosshair", "zoom", "fastScroll",
            "targetOutline", "snow", "hearts", "shulkerHud", "arrowHitSound",
            "smallSword", "smallShield", "smallTotem", "lowFire", "shulkerTooltip"
        };
        passed += verifyKeys("LEGACY EXTRAS", extras, extrasKeys,
            VisionExtras::enabled, VisionExtras::toggle,
            FabricLoader.getInstance().getConfigDir().resolve("visionmod/extras.properties"));

        String[] oreNames = {
            "Diamond", "Debris", "Iron", "Gold", "Redstone", "Emerald",
            "Lapis", "Copper", "Coal", "Quartz"
        };
        String[] oreKeys = new String[oreNames.length];
        for (int i=0;i<oreKeys.length;i++) oreKeys[i]="enabled."+i;
        passed += verifyKeys("ORES", oreNames, oreKeys,
            OreSelection::enabled, OreSelection::toggle,
            FabricLoader.getInstance().getConfigDir().resolve("visionmod-ores.properties"));

        if (passed != 54) throw new IllegalStateException(
            "[VisionMod][SELFTEST] Expected 54 toggle+save checks, got "+passed);
        if (!"FIRST".equals(VisionWorldLyrics.previewLine("FIRST | SECOND",0)) ||
            !"SECOND".equals(VisionWorldLyrics.previewLine("FIRST | SECOND",70)) ||
            VisionWorldLyrics.previewLine("",0).isBlank())
            throw new IllegalStateException("[VisionMod][SELFTEST] World Lyrics line playback failed");
        try (InputStream texture=VisionSelfTest.class.getResourceAsStream(
            "/assets/visionmod/textures/entity/pet_surface.png")) {
            if(texture==null)throw new IllegalStateException("Pet texture is not packaged");
            java.awt.image.BufferedImage image=javax.imageio.ImageIO.read(texture);
            if(image==null || image.getWidth()!=32 || image.getHeight()!=32)
                throw new IllegalStateException("Pet texture invalid 32x32 image");
        } catch(Exception e) {
            throw new IllegalStateException("[VisionMod][SELFTEST] Pet texture QA failed",e);
        }
        System.out.println("[VisionMod][SELFTEST] PASS: World Lyrics text rotation and packaged Pet texture.");
        System.out.println("[VisionMod][SELFTEST] PASS: 54 module toggles and their settings persistence.");
    }

    private static int verify(String group, String[] names, IntPredicate selected,
        IntConsumer toggle, String file, String keyPrefix) {
        String[] keys = new String[names.length];
        for (int i=0;i<names.length;i++) keys[i]=keyPrefix+i;
        Path p = FabricLoader.getInstance().getConfigDir().resolve("visionmod").resolve(file);
        return verifyKeys(group,names,keys,selected,toggle,p);
    }

    private static int verifyKeys(String group, String[] names, String[] keys,
        IntPredicate selected, IntConsumer toggle, Path p) {
        HashSet<String> seen = new HashSet<>();
        for (String name : names) {
            if (name == null || name.isBlank() || !seen.add(name))
                throw new IllegalStateException(group+": duplicate/empty module title");
        }
        if(keys.length!=names.length)throw new IllegalStateException(group+": mismatched property keys");
        for (int i = 0; i < names.length; i++) {
            boolean oldValue = selected.test(i);
            try {
                toggle.accept(i);
                if (selected.test(i) == oldValue) {
                    throw new IllegalStateException(group+"/"+names[i]+": toggle did not change state");
                }
                assertSaved(p, keys[i], !oldValue, group+"/"+names[i]);
            } finally {
                if (selected.test(i) != oldValue) toggle.accept(i);
            }
            if (selected.test(i) != oldValue)
                throw new IllegalStateException(group+"/"+names[i]+": restoration failed");
            assertSaved(p,keys[i],oldValue,group+"/"+names[i]);
        }
        return names.length;
    }

    private static void assertSaved(Path file,String key,boolean expected,String label) {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (Exception e) {
            throw new IllegalStateException(label+": could not read saved config", e);
        }
        String actual = properties.getProperty(key);
        if (!Boolean.toString(expected).equals(actual))
            throw new IllegalStateException(label+": saved "+key+"="+actual+" expected="+expected);
    }
}
