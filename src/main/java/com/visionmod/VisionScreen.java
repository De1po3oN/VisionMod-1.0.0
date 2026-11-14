package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Completely custom-drawn navigation: no stock Minecraft buttons in the main
 * screen, and every page stays in the same outer frame. MMB on a module starts
 * key capture; press a key, press MMB again, or press ESC to unbind.
 */
public final class VisionScreen extends Screen {
    public enum Page { VISUALS, ORES, EXTRAS, PERFORMANCE, CONFIGS, ADDONS }

    private static final String[] VISUALS = {
        "FullBright", "Ore Highlight", "Clear Water", "Clear Lava",
        "Player Hearts", "Particles", "Free Look"
    };
    private static final String[] EXTRAS = {
        "CPS Counter", "Armor HUD", "Food + Sat", "Custom Crosshair",
        "Zoom", "Fast Scroller", "Block Outline", "Snowflakes",
        "Air Hearts", "Shulker Summary", "Arrow Hit Sound",
        "Small Sword", "Small Shield", "Small Totem", "Low Fire", "Shulker Tooltip"
    };
    private static final String[] TITLES = {"VISUAL MODULES", "ORE SELECTION", "EXTRA MODULES", "PERFORMANCE", "CONFIG MANAGER", "INTEGRATIONS"};
    private static final String[] DETAILS = {
        "LMB: toggle   RMB: settings   MMB: bind",
        "LMB: ore   RMB: color   MMB: bind",
        "LMB: toggle   RMB: settings   MMB: bind",
        "LMB: change   MMB: bind   Adaptive only affects Vision Mod",
        "Share JSON presets with other players",
        "JEI bundled and companion mod status"
    };

    private Page page;
    private final long openedAt = System.nanoTime();
    private long closingAt = -1L;
    private long pageChangedAt = System.nanoTime();
    private static final long OPEN_NANOS = 340_000_000L;
    private static final long CLOSE_NANOS = 300_000_000L;
    private void beginClose() { if (closingAt == -1L) closingAt = System.nanoTime(); }
    private static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return 1f - (float)Math.pow(1f-t, 3);
    }
    @Override public void onClose() { beginClose(); }

    private int extrasPage;
    private int performancePage;
    private int addonsPage;
    private int captureId = -1;
    private EditBox profileInput;
    private List<String> profiles = List.of();
    private int selectedProfile;
    private String status = "Drop other players' JSON files into the folder";

    public VisionScreen() { this(Page.VISUALS); }
    public VisionScreen(boolean ores) { this(ores ? Page.ORES : Page.VISUALS); }
    public VisionScreen(Page page) {
        super(Component.literal("Vision Mod"));
        this.page = page;
    }

    @Override
    protected void init() {
        int x = VisionMenuTheme.x(width), y = VisionMenuTheme.y(height);
        profileInput = new EditBox(font, x + 129, y + 86, 302, 19,
            Component.literal("Config name"));
        profileInput.setMaxLength(32);
        profileInput.setHint(Component.literal("my_config"));
        profileInput.setVisible(page == Page.CONFIGS);
        addRenderableWidget(profileInput);
        refreshProfiles();
    }

    private void refreshProfiles() {
        String previous = currentProfile();
        profiles = VisionProfiles.list();
        selectedProfile = previous == null ? 0 : Math.max(0, profiles.indexOf(previous));
    }
    private String currentProfile() {
        return profiles.isEmpty() ? null : profiles.get(Math.min(selectedProfile, profiles.size() - 1));
    }

    private void select(Page target) {
        captureId = -1;
        page = target;
        pageChangedAt = System.nanoTime();
        if (profileInput != null) {
            profileInput.setVisible(page == Page.CONFIGS);
            profileInput.setFocused(false);
        }
        if (page == Page.CONFIGS) refreshProfiles();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        // Preserves the visible game world without a heavy framebuffer blur.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        int x = VisionMenuTheme.x(width), y = VisionMenuTheme.y(height);
        long now = System.nanoTime();
        float opening = ease((now - openedAt) / (float)OPEN_NANOS);
        float showing = closingAt < 0 ? opening : 1f - ease((now-closingAt)/(float)CLOSE_NANOS);
        if (showing < 0.005f && closingAt >= 0) { minecraft.setScreen(null); return; }
        int shadowAlpha = (int)(0x54 * showing);
        graphics.fill(0, 0, width, height, shadowAlpha << 24);
        // The whole menu, including its controls, moves as one surface.
        // Use 2D matrices (the correct graphics API for Minecraft 1.21.11).
        float scale = 0.91f + showing * 0.09f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + VisionMenuTheme.WIDTH/2f, y + VisionMenuTheme.HEIGHT/2f);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-x - VisionMenuTheme.WIDTH/2f, -y - VisionMenuTheme.HEIGHT/2f + (1f-showing)*28f);
        VisionMenuTheme.panel(graphics, font, width, height, "VISION / " + TITLES[page.ordinal()], DETAILS[page.ordinal()]);
        VisionMenuTheme.sidebar(graphics, font, x, y, page.ordinal(), mouseX, mouseY);
        graphics.fill(x + 123, y + 65, x + 124, y + 264, 0xFF34445E);

        graphics.pose().pushMatrix();
        float shift = 9f * (1f-ease((System.nanoTime()-pageChangedAt)/180_000_000f));
        graphics.pose().translate(shift,0f);
        switch (page) {
            case VISUALS -> renderVisuals(graphics, mouseX, mouseY, x, y);
            case ORES -> renderOres(graphics, mouseX, mouseY, x, y);
            case EXTRAS -> renderExtras(graphics, mouseX, mouseY, x, y);
            case PERFORMANCE -> renderPerformance(graphics, mouseX, mouseY, x, y);
            case CONFIGS -> renderConfigs(graphics, mouseX, mouseY, x, y);
            case ADDONS -> renderAddons(graphics, mouseX, mouseY, x, y);
        }
        graphics.pose().popMatrix();
        graphics.fill(x + 428, y + 6, x + 442, y + 20, 0xFF273951);
        graphics.drawCenteredString(font, "x", x + 435, y + 9, VisionMenuTheme.WHITE);
        // Native EditBox handles text input; it is the only Minecraft widget on this screen.
        super.render(graphics, mouseX, mouseY, tickDelta);
        if (captureId >= 0) {
            graphics.fill(x + 127, y + 125, x + 440, y + 195, 0xF5081322);
            graphics.fillGradient(x + 127, y + 125, x + 440, y + 128,
                VisionMenuTheme.CYAN, VisionMenuTheme.PURPLE);
            graphics.drawCenteredString(font, "PRESS A KEY TO BIND", x + 283, y + 142, VisionMenuTheme.WHITE);
            graphics.drawCenteredString(font, "ESC clears  |  Mouse buttons 3-5 supported",
                x + 283, y + 163, VisionMenuTheme.MUTED);
        }
        if (closingAt >= 0) {
            int cover = Math.min(245, (int)((1f-showing)*236));
            graphics.fill(x, y, x + VisionMenuTheme.WIDTH, y + VisionMenuTheme.HEIGHT, cover << 24);
        }
        graphics.pose().popMatrix();
    }

    private static int colX(int x, int local) { return x + 130 + local % 2 * 155; }
    private static int rowY(int y, int local) { return y + 74 + local / 2 * 46; }
    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return VisionMenuTheme.inside(mx, my, x, y, w, h);
    }

    private boolean visualEnabled(int index) {
        return switch (index) {
            case 0 -> VisionModClient.fullBright;
            case 1 -> VisionModClient.oreHighlight;
            case 2 -> VisionModClient.clearWater;
            case 3 -> VisionModClient.clearLava;
            case 4 -> VisionModClient.playerHearts;
            case 5 -> VisionModClient.particles;
            case 6 -> FreeLook.enabled;
            default -> false;
        };
    }

    private void visualToggle(int index) {
        switch (index) {
            case 0 -> VisionModClient.fullBright = !VisionModClient.fullBright;
            case 1 -> VisionModClient.oreHighlight = !VisionModClient.oreHighlight;
            case 2 -> VisionModClient.clearWater = !VisionModClient.clearWater;
            case 3 -> VisionModClient.clearLava = !VisionModClient.clearLava;
            case 4 -> VisionModClient.playerHearts = !VisionModClient.playerHearts;
            case 5 -> VisionModClient.particles = !VisionModClient.particles;
            case 6 -> FreeLook.enabled = !FreeLook.enabled;
            default -> { return; }
        }
        VisionPerformance.custom();
    }

    private void renderVisuals(GuiGraphics g, int mx, int my, int x, int y) {
        for (int i = 0; i < VISUALS.length; i++) {
            int bx = colX(x, i), by = rowY(y, i);
            VisionMenuTheme.moduleRow(g, font, bx, by, 149, 41,
                VISUALS[i], visualEnabled(i), VisionHotkeys.label(VisionHotkeys.visual(i)),
                hit(mx, my, bx, by, 149, 41));
        }
        VisionMenuTheme.button(g, font, x + 130, y + 264, 304, 11,
            "Right Shift: menu     MMB: rebind", hit(mx, my, x + 130, y + 264, 304, 11));
    }

    private void renderOres(GuiGraphics g, int mx, int my, int x, int y) {
        for (int i = 0; i < OreColors.NAMES.length; i++) {
            int bx = x + 130 + (i / 5) * 155;
            int by = y + 70 + (i % 5) * 38;
            boolean on = OreSelection.enabled(i);
            int w = 149;
            g.fill(bx, by, bx + w, by + 35,
                hit(mx, my, bx, by, w, 35) ? VisionMenuTheme.HOVER : VisionMenuTheme.SURFACE);
            g.fill(bx, by, bx + 3, by + 35, on ? VisionMenuTheme.CYAN : 0xFF596C82);
            g.drawString(font, OreColors.NAMES[i], bx + 8, by + 6, VisionMenuTheme.WHITE);
            g.drawString(font, VisionHotkeys.label(VisionHotkeys.ore(i)), bx + 8, by + 21, VisionMenuTheme.MUTED);
            g.fill(bx + 111, by + 8, bx + 128, by + 25, OreColors.getColor(i));
            g.fill(bx + 134, by + 10, bx + 145, by + 22,
                on ? VisionMenuTheme.CYAN : 0xFF637089);
        }
        g.drawString(font, "Selected: " + enabledOres() + " / 10", x + 132, y + 266, VisionMenuTheme.CYAN);
    }

    private int enabledOres() {
        int count = 0;
        for (int i = 0; i < OreColors.NAMES.length; i++) if (OreSelection.enabled(i)) count++;
        return count;
    }

    private static final String[] EXTRA_SETTINGS = {
        "Zoom factor", "Outline color", "Crosshair color",
        "Heart FX mode", "Ambient density", "Smooth Free Look"
    };
    private static final String[] PERFORMANCE_VALUES = {
        "Ore radius", "Checks / tick", "Max outlines", "FPS target", "Busy threshold"
    };
    private static final String[] PERFORMANCE_SWITCHES = {
        "Adaptive budgets", "Reduce HUD at spawn", "Limit ambient rate"
    };
    private static final String[] PRESET_NAMES = {"MAX FPS", "BALANCED", "VISUAL"};
    private static final String[] PRESET_VALUES = {"MAX_FPS", "BALANCED", "VISUAL"};

    private String extraSettingValue(int index) {
        return switch (index) {
            case 0 -> Math.round(VisionExtras.zoomFactor * 100) + "%";
            case 1 -> String.format("#%06X", VisionExtras.outlineColor & 0xFFFFFF);
            case 2 -> String.format("#%06X", VisionExtras.crosshairColor & 0xFFFFFF);
            case 3 -> VisionExtras.ambientStyleLabel();
            case 4 -> VisionExtras.ambientDensityLabel();
            case 5 -> FreeLook.smooth ? "ON" : "OFF";
            default -> "";
        };
    }

    private String performanceValue(int index) {
        return switch (index) {
            case 0 -> Integer.toString(VisionPerformance.oreRadius);
            case 1 -> Integer.toString(VisionPerformance.checksPerTick);
            case 2 -> Integer.toString(VisionPerformance.outlineLimit);
            case 3 -> Integer.toString(VisionRenderBoost.targetFps);
            case 4 -> Integer.toString(VisionPerformance.busyPlayerThreshold);
            default -> "";
        };
    }

    private void renderExtras(GuiGraphics g, int mx, int my, int x, int y) {
        if (extrasPage < 2) {
            int first = extrasPage * 8, end = Math.min(first + 8, EXTRAS.length);
            for (int i = first; i < end; i++) {
                int local = i - first, bx = colX(x, local), by = rowY(y, local);
                VisionMenuTheme.moduleRow(g, font, bx, by, 149, 41, EXTRAS[i], VisionExtras.enabled(i),
                    VisionHotkeys.label(VisionHotkeys.extra(i)), hit(mx, my, bx, by, 149, 41));
            }
        } else if (extrasPage == 2) {
            for (int i = 0; i < EXTRA_SETTINGS.length; i++) {
                int bx = colX(x, i), by = rowY(y, i);
                VisionMenuTheme.actionRow(g, font, bx, by, 149, 41,
                    EXTRA_SETTINGS[i] + ": " + extraSettingValue(i),
                    i < 3 ? VisionHotkeys.label(VisionHotkeys.extraSetting(i)) : "LMB: cycle",
                    hit(mx, my, bx, by, 149, 41));
            }
            g.drawString(font, "Right-click crosshair module to draw its shape",
                x + 130, y + 227, VisionMenuTheme.MUTED);
        } else if (extrasPage == 3) {
            for (int i = 0; i < VisionFeaturePack.count(); i++) {
                int bx = colX(x, i), by = rowY(y, i);
                VisionMenuTheme.moduleRow(g, font, bx, by, 149, 41,
                    VisionFeaturePack.NAMES[i], VisionFeaturePack.enabled(i),
                    "LMB: toggle", hit(mx, my, bx, by, 149, 41));
            }
        } else if (extrasPage == 4) {
            for (int i = 0; i < VisionWorldEffects.count(); i++) {
                int bx = colX(x, i), by = rowY(y, i);
                VisionMenuTheme.moduleRow(g, font, bx, by, 149, 41,
                    VisionWorldEffects.NAMES[i], VisionWorldEffects.enabled(i),
                    "LMB: toggle", hit(mx, my, bx, by, 149, 41));
            }
            g.drawString(font, "World-space effects (optional, bounded render budget)",
                x + 131, y + 211, VisionMenuTheme.MUTED);
        } else {
            for (int i = 0; i < VisionExtendedFeatures.count(); i++) {
                int bx = colX(x, i), by = rowY(y, i);
                VisionMenuTheme.moduleRow(g, font, bx, by, 149, 41,
                    VisionExtendedFeatures.NAMES[i], VisionExtendedFeatures.enabled(i),
                    "LMB: toggle", hit(mx, my, bx, by, 149, 41));
            }
            g.drawString(font, "Set ownLyric / friends in config/visionmod/extended-2.properties",
                x + 130, y + 254, VisionMenuTheme.MUTED);
        }
        String subsection = extrasPage == 2 ? "НАСТРОЙКИ" :
            extrasPage == 3 ? "2.0 / HUD & COMBAT" :
            extrasPage == 4 ? "2.0 / WORLD FX" :
            extrasPage == 5 ? "2.0 / EXTRA FX" : "МОДУЛИ " + (extrasPage + 1) + "/2";
        g.drawString(font, "EXTRAS / " + subsection, x + 131, y + 59, VisionMenuTheme.PAGE_GOLD);
        VisionMenuTheme.pageNavigator(g, font, x, y, extrasPage, 6, mx, my);
    }

    private void renderPerformance(GuiGraphics g, int mx, int my, int x, int y) {
        if (performancePage == 0) {
            for (int i = 0; i < PRESET_NAMES.length; i++) {
                int bx = colX(x, i), by = rowY(y, i);
                VisionMenuTheme.moduleRow(g, font, bx, by, 149, 41,
                    "Preset " + PRESET_NAMES[i],
                    VisionPerformance.preset.equals(PRESET_VALUES[i]),
                    VisionHotkeys.label(VisionHotkeys.preset(i)), hit(mx, my, bx, by, 149, 41));
            }
            for (int i = 0; i < PERFORMANCE_SWITCHES.length; i++) {
                int local = i + PRESET_NAMES.length, bx = colX(x, local), by = rowY(y, local);
                VisionMenuTheme.moduleRow(g, font, bx, by, 149, 41,
                    PERFORMANCE_SWITCHES[i], VisionHotkeys.performanceEnabled(i),
                    VisionHotkeys.label(VisionHotkeys.performanceToggle(i)),
                    hit(mx, my, bx, by, 149, 41));
            }
        } else {
            for (int i = 0; i < PERFORMANCE_VALUES.length; i++) {
                int bx = colX(x, i), by = rowY(y, i);
                VisionMenuTheme.actionRow(g, font, bx, by, 149, 41,
                    PERFORMANCE_VALUES[i] + ": " + performanceValue(i),
                    VisionHotkeys.label(VisionHotkeys.performanceValue(i)),
                    hit(mx, my, bx, by, 149, 41));
            }
            int bx=colX(x,5), by=rowY(y,5);
            VisionMenuTheme.moduleRow(g,font,bx,by,149,41,"REAL FPS BOOST",
                VisionRenderBoost.enabled, VisionHotkeys.label(VisionHotkeys.boostToggle()),
                hit(mx,my,bx,by,149,41));
            VisionMenuTheme.option(g,font,x+130,y+211,304,20,
                "RENDER DISTANCE CAP: "+VisionRenderBoost.chunkCap+" CHUNKS  |  MMB: BIND",
                false,hit(mx,my,x+130,y+211,304,20));
        }
        String info = "Players " + VisionPerformance.loadedPlayers() +
            "  |  ~" + VisionPerformance.estimatedFps() + " FPS  |  " +
            (VisionPerformance.busy() ? "THROTTLED" : "NORMAL");
        if (performancePage == 0)
            g.drawString(font, info, x + 130, y + 219,
                VisionPerformance.busy() ? VisionMenuTheme.RED : VisionMenuTheme.CYAN);
        g.drawString(font, performancePage == 0 ? "PERFORMANCE / ПРЕСЕТЫ" : "PERFORMANCE / ЛИМИТЫ",
            x + 131, y + 59, VisionMenuTheme.PAGE_GOLD);
        VisionMenuTheme.pageNavigator(g, font, x, y, performancePage, 2, mx, my);
    }


    private static final String[] COMPANION_NAMES = {"JEI (BUNDLED)", "FIRST PERSON", "APPLE SKIN", "SKYBOXIFY", "RESCALE"};
    private static final String[] COMPANION_IDS = {"jei", "firstperson", "appleskin", "skyboxify", "rescale"};
    private static final String[] COMPANION_PAGES = {
        "https://modrinth.com/mod/jei",
        "https://modrinth.com/mod/first-person-model",
        "https://modrinth.com/mod/appleskin",
        "https://modrinth.com/mod/skyboxify",
        "https://modrinth.com/mod/rescale"
    };

    private void renderAddons(GuiGraphics g, int mx, int my, int x, int y) {
        if (addonsPage == 0) {
            VisionMenuTheme.moduleRow(g,font,colX(x,0),rowY(y,0),149,41,
                "TOTEM ANIMATION", VisionAdvanced.smallTotemAnimation,
                VisionHotkeys.label(VisionHotkeys.totemAnimationToggle()),
                hit(mx,my,colX(x,0),rowY(y,0),149,41));
            VisionMenuTheme.actionRow(g,font,colX(x,1),rowY(y,1),149,41,
                "SIZE: " + VisionAdvanced.totemAnimationPercent + "%",
                VisionHotkeys.label(VisionHotkeys.totemAnimationSize()),
                hit(mx,my,colX(x,1),rowY(y,1),149,41));
            VisionMenuTheme.moduleRow(g,font,colX(x,2),rowY(y,2),149,41,
                "SHULKER 9x3 GRID", VisionAdvanced.shulkerGrid,
                VisionHotkeys.label(VisionHotkeys.shulkerGridToggle()),
                hit(mx,my,colX(x,2),rowY(y,2),149,41));
            VisionMenuTheme.actionRow(g,font,colX(x,3),rowY(y,3),149,41,
                "RESOURCE PACKS", "Import custom skies",
                hit(mx,my,colX(x,3),rowY(y,3),149,41));
            VisionMenuTheme.actionRow(g,font,colX(x,4),rowY(y,4),149,41,
                "MODS FOLDER", "For companion JARs",
                hit(mx,my,colX(x,4),rowY(y,4),149,41));
            g.drawString(font, "Built-in controls are independent of extra mods",
                x+130,y+221,VisionMenuTheme.MUTED);
        } else {
            for (int i=0;i<COMPANION_NAMES.length;i++) {
                int bx=colX(x,i),by=rowY(y,i);
                boolean loaded=VisionAdvanced.installed(COMPANION_IDS[i]);
                VisionMenuTheme.actionRow(g,font,bx,by,149,41,
                    COMPANION_NAMES[i],loaded?"INSTALLED":"OPEN OFFICIAL PAGE",
                    hit(mx,my,bx,by,149,41));
                g.fill(bx+142,by+5,bx+147,by+12,loaded?VisionMenuTheme.GREEN:VisionMenuTheme.PAGE_GOLD);
            }
            g.drawString(font,"JEI is bundled; other addons are optional",
                x+130,y+220,VisionMenuTheme.MUTED);
        }
        g.drawString(font,addonsPage==0?"ADVANCED / IN-GAME":"COMPATIBILITY / OPTIONAL MODS",
            x+131,y+59,VisionMenuTheme.PAGE_GOLD);
        VisionMenuTheme.pageNavigator(g,font,x,y,addonsPage,2,mx,my);
    }

    private void renderConfigs(GuiGraphics g, int mx, int my, int x, int y) {
        g.drawString(font, "NEW PROFILE NAME", x + 130, y + 72, VisionMenuTheme.MUTED);
        String selected = currentProfile() == null ? "(none)" : currentProfile();
        VisionMenuTheme.button(g, font, x + 130, y + 117, 304, 25,
            "SELECT: " + selected, hit(mx, my, x + 130, y + 117, 304, 25));
        String[] actions = {"SAVE CURRENT", "LOAD SELECTED", "DELETE", "REFRESH"};
        for (int i = 0; i < actions.length; i++) {
            int bx = x + 130 + i % 2 * 155, by = y + 150 + i / 2 * 34;
            VisionMenuTheme.button(g, font, bx, by, 149, 28,
                actions[i], hit(mx, my, bx, by, 149, 28));
        }
        VisionMenuTheme.option(g, font, x + 130, y + 226, 304, 28,
            "OPEN CONFIGS FOLDER", true, hit(mx, my, x + 130, y + 226, 304, 28));
        String shortStatus = status;
        while (!shortStatus.isEmpty() && font.width(shortStatus) > 303)
            shortStatus = shortStatus.substring(0, shortStatus.length() - 1);
        g.drawString(font, shortStatus, x + 130, y + 260, VisionMenuTheme.MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (closingAt >= 0 || System.nanoTime()-openedAt < OPEN_NANOS) return true;
        int x = VisionMenuTheme.x(width), y = VisionMenuTheme.y(height);
        double mx = click.x(), my = click.y();
        int button = click.button();
        if (captureId >= 0) {
            // MMB again binds MMB itself; left/right also possible but often conflict with mining.
            VisionHotkeys.assign(captureId, -button - 1);
            captureId = -1;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int color = VisionMenuTheme.paletteHit(mx,my,width,height);
            if (color >= 0) { VisionMenuTheme.setPalette(color); return true; }
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && hit(mx, my, x + 428, y + 6, 14, 14)) {
            beginClose();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int i = 0; i < Page.values().length; i++) {
                if (hit(mx, my, x + 18, y + 70 + i * 32, 93, 27)) {
                    select(Page.values()[i]);
                    return true;
                }
            }
        }
        switch (page) {
            case VISUALS -> {
                for (int i = 0; i < VISUALS.length; i++) {
                    int bx = colX(x, i), by = rowY(y, i);
                    if (!hit(mx, my, bx, by, 149, 41)) continue;
                    if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) { captureId = VisionHotkeys.visual(i); return true; }
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) { visualToggle(i); return true; }
                    if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && i == 1) { select(Page.ORES); return true; }
                    if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && i == 5) { minecraft.setScreen(new ParticleSettingsScreen()); return true; }
                    if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && i == 6) {
                        FreeLook.smooth = !FreeLook.smooth;
                        VisionExtras.save();
                        return true;
                    }
                }
            }
            case ORES -> {
                for (int i = 0; i < OreColors.NAMES.length; i++) {
                    int bx = x + 130 + (i / 5) * 155, by = y + 70 + (i % 5) * 38;
                    if (!hit(mx, my, bx, by, 149, 35)) continue;
                    if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) { captureId = VisionHotkeys.ore(i); return true; }
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) { OreSelection.toggle(i); return true; }
                    if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { minecraft.setScreen(new OreColorPickerScreen(i)); return true; }
                }
            }
            case EXTRAS -> {
                if (extrasPage < 2) {
                    int first = extrasPage * 8, last = Math.min(first + 8, EXTRAS.length);
                    for (int i = first; i < last; i++) {
                        int bx = colX(x, i - first), by = rowY(y, i - first);
                        if (!hit(mx, my, bx, by, 149, 41)) continue;
                        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) { captureId = VisionHotkeys.extra(i); return true; }
                        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) { VisionExtras.toggle(i); return true; }
                        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && i == 3) {
                            minecraft.setScreen(new VisionCrosshairEditorScreen()); return true;
                        }
                        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && i == 4) { VisionExtras.cycleZoom(); return true; }
                        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && i == 6) { VisionExtras.cycleOutlineColor(); return true; }
                        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && (i == 7 || i == 8)) {
                            extrasPage = 2;
                            pageChangedAt = System.nanoTime();
                            return true;
                        }
                    }
                } else if (extrasPage == 2) {
                    for (int i = 0; i < EXTRA_SETTINGS.length; i++) {
                        int bx = colX(x, i), by = rowY(y, i);
                        if (!hit(mx, my, bx, by, 149, 41)) continue;
                        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && i < 3) {
                            captureId = VisionHotkeys.extraSetting(i);
                            return true;
                        }
                        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                            switch (i) {
                                case 3 -> VisionExtras.cycleAmbientStyle();
                                case 4 -> VisionExtras.cycleAmbientDensity();
                                case 5 -> { FreeLook.smooth = !FreeLook.smooth; VisionExtras.save(); }
                                default -> VisionHotkeys.cycleExtraSetting(i);
                            }
                            return true;
                        }
                    }
                } else if (extrasPage == 3) {
                    for (int i = 0; i < VisionFeaturePack.count(); i++) {
                        int bx = colX(x, i), by = rowY(y, i);
                        if (hit(mx, my, bx, by, 149, 41)) {
                            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) VisionFeaturePack.toggle(i);
                            return true;
                        }
                    }
                } else if (extrasPage == 4) {
                    for (int i = 0; i < VisionWorldEffects.count(); i++) {
                        int bx = colX(x, i), by = rowY(y, i);
                        if (hit(mx, my, bx, by, 149, 41)) {
                            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) VisionWorldEffects.toggle(i);
                            return true;
                        }
                    }
                } else if (extrasPage == 5) {
                    for (int i = 0; i < VisionExtendedFeatures.count(); i++) {
                        int bx = colX(x, i), by = rowY(y, i);
                        if (hit(mx, my, bx, by, 149, 41)) {
                            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                                if (i == 2 && VisionExtendedFeatures.enabled(i)) {
                                    VisionExtendedFeatures.cycleWorldColor();
                                } else {
                                    VisionExtendedFeatures.toggle(i);
                                }
                            }
                            return true;
                        }
                    }
                }
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    int step = VisionMenuTheme.pageNavigationHit(mx, my, x, y, extrasPage, 6);
                    if (step != 0) {
                        extrasPage += step;
                        pageChangedAt = System.nanoTime();
                        return true;
                    }
                }
            }
            case PERFORMANCE -> {
                if (performancePage == 0) {
                    for (int i = 0; i < 3; i++) {
                        int bx = colX(x, i), by = rowY(y, i);
                        if (!hit(mx, my, bx, by, 149, 41)) continue;
                        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) { captureId = VisionHotkeys.preset(i); return true; }
                        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) { VisionHotkeys.applyPreset(i); return true; }
                    }
                    for (int i = 0; i < 3; i++) {
                        int bx = colX(x, i + 3), by = rowY(y, i + 3);
                        if (!hit(mx, my, bx, by, 149, 41)) continue;
                        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) { captureId = VisionHotkeys.performanceToggle(i); return true; }
                        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) { VisionHotkeys.togglePerformance(i); return true; }
                    }
                } else {
                    for (int i = 0; i < PERFORMANCE_VALUES.length; i++) {
                        int bx = colX(x, i), by = rowY(y, i);
                        if (!hit(mx, my, bx, by, 149, 41)) continue;
                        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) { captureId = VisionHotkeys.performanceValue(i); return true; }
                        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) { VisionHotkeys.cyclePerformanceValue(i); return true; }
                    }
                    if (hit(mx,my,colX(x,5),rowY(y,5),149,41)) {
                        if (button==GLFW.GLFW_MOUSE_BUTTON_MIDDLE) captureId=VisionHotkeys.boostToggle();
                        if (button==GLFW.GLFW_MOUSE_BUTTON_LEFT) VisionRenderBoost.toggle();
                        return true;
                    }
                    if (hit(mx,my,x+130,y+211,304,20)) {
                        if (button==GLFW.GLFW_MOUSE_BUTTON_MIDDLE) captureId=VisionHotkeys.boostCap();
                        if (button==GLFW.GLFW_MOUSE_BUTTON_LEFT) VisionRenderBoost.cycleCap();
                        return true;
                    }
                }
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    int step = VisionMenuTheme.pageNavigationHit(mx, my, x, y, performancePage, 2);
                    if (step != 0) {
                        performancePage += step;
                        pageChangedAt = System.nanoTime();
                        return true;
                    }
                }
            }

            case ADDONS -> {
                if (addonsPage == 0) {
                    for (int i=0;i<5;i++) {
                        int bx=colX(x,i),by=rowY(y,i);
                        if (!hit(mx,my,bx,by,149,41)) continue;
                        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && i < 3) {
                            captureId = switch(i) {
                                case 0 -> VisionHotkeys.totemAnimationToggle();
                                case 1 -> VisionHotkeys.totemAnimationSize();
                                default -> VisionHotkeys.shulkerGridToggle();
                            };
                            return true;
                        }
                        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                            switch(i) {
                                case 0 -> { VisionAdvanced.smallTotemAnimation = !VisionAdvanced.smallTotemAnimation; VisionAdvanced.save(); }
                                case 1 -> VisionAdvanced.cycleTotemSize();
                                case 2 -> { VisionAdvanced.shulkerGrid = !VisionAdvanced.shulkerGrid; VisionAdvanced.save(); }
                                case 3 -> status = VisionAdvanced.openResourcepacksFolder() ? "Resource packs opened" : "Open resourcepacks manually";
                                case 4 -> status = VisionAdvanced.openModsFolder() ? "Mods folder opened" : "Open mods manually";
                                default -> { }
                            }
                            return true;
                        }
                    }
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    for (int i=0;i<COMPANION_NAMES.length;i++) {
                        if (!hit(mx,my,colX(x,i),rowY(y,i),149,41)) continue;
                        VisionAdvanced.openOfficialPage(COMPANION_PAGES[i]);
                        return true;
                    }
                }
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    int step=VisionMenuTheme.pageNavigationHit(mx,my,x,y,addonsPage,2);
                    if (step != 0) {
                        addonsPage += step;
                        pageChangedAt = System.nanoTime();
                        return true;
                    }
                }
            }
            case CONFIGS -> {
                if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) break;
                if (hit(mx, my, x + 130, y + 117, 304, 25)) {
                    refreshProfiles();
                    if (!profiles.isEmpty()) selectedProfile = (selectedProfile + 1) % profiles.size();
                    return true;
                }
                for (int i = 0; i < 4; i++) {
                    int bx = x + 130 + i % 2 * 155, by = y + 150 + i / 2 * 34;
                    if (!hit(mx, my, bx, by, 149, 28)) continue;
                    try {
                        switch (i) {
                            case 0 -> { VisionProfiles.save(profileInput.getValue().trim()); status = "Profile saved"; refreshProfiles(); }
                            case 1 -> { VisionProfiles.load(currentProfile()); status = "Profile loaded"; }
                            case 2 -> { VisionProfiles.delete(currentProfile()); status = "Profile removed"; refreshProfiles(); }
                            case 3 -> { refreshProfiles(); status = "List refreshed"; }
                            default -> { }
                        }
                    } catch (Exception e) { status = "Error: " + e.getMessage(); }
                    return true;
                }
                if (hit(mx, my, x + 130, y + 226, 304, 28)) {
                    status = VisionProfiles.openFolder() ? "Folder opened" : "config/visionmod/profiles";
                    refreshProfiles();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (closingAt >= 0) return true;
        if (captureId >= 0) {
            int code = event.key();
            VisionHotkeys.assign(captureId, code == GLFW.GLFW_KEY_ESCAPE ? 0 : code);
            captureId = -1;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public boolean isPauseScreen() { return false; }
}
