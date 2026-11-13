package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Unified page for separately switchable lightweight modules. */
public final class VisionExtrasScreen extends Screen {
    private static final String[] NAMES = {
        "CPS COUNTER", "ARMOR HUD",
        "FOOD + SAT", "CROSSHAIR",
        "ZOOM (C)", "FAST SCROLL",
        "BLOCK OUTLINE", "SNOWFLAKES",
        "AIR HEARTS", "SHULKER HUD", "ARROW SOUND",
        "SMALL SWORD", "SMALL SHIELD", "SMALL TOTEM", "LOW FIRE"
    };
    private boolean secondPage;
    public VisionExtrasScreen() { super(Component.literal("Vision Mod / Extras")); }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float delta) { }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        int x = VisionMenuTheme.x(width), y = VisionMenuTheme.y(height);
        VisionMenuTheme.panel(g, font, width, height, secondPage ? "VISION / EXTRAS 2/2" : "VISION / EXTRAS 1/2",
            "Click to toggle; render tweaks are experimental");
        int first = secondPage ? 8 : 0;
        int last = Math.min(first + 8, NAMES.length);
        for (int i = first; i < last; i++) {
            int local = i - first;
            int bx = x + 12 + (local % 2) * 159, by = y + 50 + (local / 2) * 27;
            VisionMenuTheme.toggle(g, font, bx, by, 153, 24, NAMES[i], VisionExtras.enabled(i),
                VisionMenuTheme.inside(mx, my, bx, by, 153, 24));
        }
        VisionMenuTheme.button(g, font, x + 12, y + 165, 312, 19,
            secondPage ? "BACK TO PAGE 1" : "MORE SETTINGS  >",
            VisionMenuTheme.inside(mx,my,x+12,y+165,312,19));
        VisionMenuTheme.button(g, font, x + 12, y + 190, 99, 19, "DRAW RETICLE", VisionMenuTheme.inside(mx,my,x+12,y+190,99,19));
        VisionMenuTheme.button(g, font, x + 119, y + 190, 99, 19, "BLOCK COLOR", VisionMenuTheme.inside(mx,my,x+119,y+190,99,19));
        VisionMenuTheme.button(g, font, x + 226, y + 190, 98, 19, "ZOOM: " + (int)(100*VisionExtras.zoomFactor) + "%", VisionMenuTheme.inside(mx,my,x+226,y+190,98,19));
        VisionMenuTheme.button(g, font, x + 12, y + 209, 312, 18, "BACK TO VISUALS", VisionMenuTheme.inside(mx,my,x+12,y+209,312,18));
    }
    @Override public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(click, doubled);
        int x = VisionMenuTheme.x(width), y = VisionMenuTheme.y(height);
        double mx = click.x(), my = click.y();
        int first = secondPage ? 8 : 0;
        int last = Math.min(first + 8, NAMES.length);
        for (int i = first; i < last; i++) {
            int local = i - first;
            int bx = x + 12 + (local % 2)*159, by = y + 50 + (local/2)*27;
            if (VisionMenuTheme.inside(mx,my,bx,by,153,24)) { VisionExtras.toggle(i); return true; }
        }
        if (VisionMenuTheme.inside(mx,my,x+12,y+165,312,19)) { secondPage=!secondPage;return true; }
        if (VisionMenuTheme.inside(mx,my,x+12,y+190,99,19)) { minecraft.setScreen(new VisionCrosshairEditorScreen()); return true; }
        if (VisionMenuTheme.inside(mx,my,x+119,y+190,99,19)) { VisionExtras.cycleOutlineColor(); return true; }
        if (VisionMenuTheme.inside(mx,my,x+226,y+190,98,19)) { VisionExtras.cycleZoom(); return true; }
        if (VisionMenuTheme.inside(mx,my,x+12,y+209,312,18)) { minecraft.setScreen(new VisionScreen()); return true; }
        return super.mouseClicked(click,doubled);
    }
    @Override public void onClose() { minecraft.setScreen(new VisionScreen()); }
    @Override public boolean isPauseScreen(){return false;}
}
