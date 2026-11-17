package com.visionmod;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/** An isolated mod font; the game's chat, item names and resource-pack fonts are untouched. */
public final class VisionFonts {
    private static final Style UI = Style.EMPTY.withFont(new FontDescription.Resource(
        Identifier.fromNamespaceAndPath("visionmod", "ui")));
    private VisionFonts() { }
    public static MutableComponent text(String value) { return Component.literal(value).withStyle(UI); }
    public static int width(Font font, String value) { return font.width(text(value)); }
    public static void draw(GuiGraphics g, Font font, String value, int x, int y, int color) {
        g.drawString(font, text(value), x, y, color, false);
    }
    public static void fit(GuiGraphics g, Font font, String value, int x, int y, int width, int color) {
        if (width <= 0) return;
        if (width(font, value) > width) {
            if (width(font, "…") > width) return;
            int low = 0, high = value.codePointCount(0, value.length());
            while (low < high) {
                int middle = (low + high + 1) / 2;
                String candidate = value.substring(0, value.offsetByCodePoints(0, middle)) + "…";
                if (width(font, candidate) <= width) low = middle; else high = middle - 1;
            }
            value = value.substring(0, value.offsetByCodePoints(0, low)) + "…";
        }
        draw(g, font, value, x, y, color);
    }
}
