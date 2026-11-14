package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Unified particle filter screen: LMB toggle, MMB bind, ESC clear bind. */
public final class ParticleSettingsScreen extends Screen {
    private static final String[] NAMES = {
        "Block breaking", "Sprint", "Ender chest / Portal", "ALL particles"
    };
    private int captureId = -1;

    public ParticleSettingsScreen() {
        super(Component.literal("Vision / Particles"));
    }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float delta) { }

    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        VisionMenuTheme.panel(g, font, width, height, "VISION / PARTICLES",
            "LMB: hide/show effects    MMB: bind a key");
        int x = VisionMenuTheme.x(width), y = VisionMenuTheme.y(height);
        for (int i = 0; i < NAMES.length; i++) {
            int bx = x + 72 + (i % 2) * 155, by = y + 87 + (i / 2) * 64;
            VisionMenuTheme.moduleRow(g, font, bx, by, 149, 49, NAMES[i],
                VisionHotkeys.particleEnabled(i),
                VisionHotkeys.label(VisionHotkeys.particle(i)),
                VisionMenuTheme.inside(mx, my, bx, by, 149, 49));
        }
        VisionMenuTheme.button(g, font, x + 73, y + 245, 304, 24,
            "BACK TO VISUALS", VisionMenuTheme.inside(mx, my, x + 73, y + 245, 304, 24));
        super.render(g, mx, my, delta);
        if (captureId >= 0) {
            g.fill(x + 63, y + 121, x + 392, y + 188, 0xF5081322);
            g.fillGradient(x + 63, y + 121, x + 392, y + 124,
                VisionMenuTheme.CYAN, VisionMenuTheme.PURPLE);
            g.drawCenteredString(font, "PRESS A KEY TO BIND", x + 227, y + 139,
                VisionMenuTheme.WHITE);
            g.drawCenteredString(font, "ESC clears binding  |  Mouse buttons supported",
                x + 227, y + 161, VisionMenuTheme.MUTED);
        }
    }

    @Override public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int x = VisionMenuTheme.x(width), y = VisionMenuTheme.y(height);
        int button = click.button();
        if (captureId >= 0) {
            VisionHotkeys.assign(captureId, -button - 1);
            captureId = -1;
            return true;
        }
        for (int i = 0; i < NAMES.length; i++) {
            int bx = x + 72 + (i % 2) * 155, by = y + 87 + (i / 2) * 64;
            if (!VisionMenuTheme.inside(click.x(), click.y(), bx, by, 149, 49)) continue;
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                captureId = VisionHotkeys.particle(i);
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                VisionHotkeys.toggleParticle(i);
                return true;
            }
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT &&
            VisionMenuTheme.inside(click.x(), click.y(), x + 73, y + 245, 304, 24)) {
            minecraft.setScreen(new VisionDashboardScreen());
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (captureId >= 0) {
            int code = event.key();
            VisionHotkeys.assign(captureId, code == GLFW.GLFW_KEY_ESCAPE ? 0 : code);
            captureId = -1;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public void onClose() {
        minecraft.setScreen(new VisionDashboardScreen());
    }

    @Override public boolean isPauseScreen() { return false; }
}
