package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** FPS presets and independent performance toggles in the shared menu layout. */
public final class PerformanceScreen extends Screen {
    private Button presetButton, oreButton, radiusButton, budgetButton, limitButton;
    private Button heartsButton, particleButton, allParticleButton, targetFpsButton;

    public PerformanceScreen() {
        super(Component.literal("Vision Mod / Performance"));
    }

    private static Component label(String text) { return Component.literal(text); }
    private static String flag(boolean value) { return value ? "ON" : "OFF"; }

    private static int next(int value, int... variants) {
        for (int i = 0; i < variants.length; i++) {
            if (value == variants[i]) return variants[(i + 1) % variants.length];
        }
        return variants[0];
    }

    private Button add(int left, int top, int w, String caption,
                       java.util.function.Consumer<Button> onClick) {
        return addRenderableWidget(Button.builder(label(caption), b -> onClick.accept(b))
            .bounds(left, top, w, 20).build());
    }

    @Override
    protected void init() {
        int x = VisionMenuTheme.x(width), y = VisionMenuTheme.y(height);
        presetButton = add(x + 12, y + 51, 312, "PRESET", b -> {
            switch (VisionPerformance.preset) {
                case "MAX_FPS" -> VisionPerformance.balanced();
                case "BALANCED" -> VisionPerformance.visual();
                default -> VisionPerformance.maxFps();
            }
            refresh();
        });
        oreButton = add(x + 12, y + 79, 153, "ORE", b -> {
            VisionModClient.oreHighlight = !VisionModClient.oreHighlight; changed();
        });
        radiusButton = add(x + 171, y + 79, 153, "RADIUS", b -> {
            VisionPerformance.oreRadius = next(VisionPerformance.oreRadius, 4, 6, 8, 12, 16); changed();
        });
        budgetButton = add(x + 12, y + 103, 153, "BUDGET", b -> {
            VisionPerformance.checksPerTick = next(VisionPerformance.checksPerTick, 40, 80, 160, 320); changed();
        });
        limitButton = add(x + 171, y + 103, 153, "OUTLINES", b -> {
            VisionPerformance.outlineLimit = next(VisionPerformance.outlineLimit, 32, 48, 96, 160, 240); changed();
        });
        heartsButton = add(x + 12, y + 151, 153, "HEARTS", b -> {
            VisionModClient.playerHearts = !VisionModClient.playerHearts; changed();
        });
        targetFpsButton = add(x + 171, y + 151, 153, "FPS TARGET", b -> {
            VisionRenderBoost.cycleTargetFps(); changed();
        });
        particleButton = add(x + 12, y + 175, 153, "PARTICLES", b -> {
            VisionModClient.particles = !VisionModClient.particles; changed();
        });
        allParticleButton = add(x + 171, y + 175, 153, "HIDE ALL", b -> {
            VisionModClient.hideAllParticles = !VisionModClient.hideAllParticles; changed();
        });
        add(x + 12, y + 204, 312, "BACK TO VISUALS",
            b -> minecraft.setScreen(new VisionDashboardScreen()));
        refresh();
    }

    private void changed() { VisionPerformance.custom(); refresh(); }

    private void refresh() {
        presetButton.setMessage(label("PRESET: " + VisionPerformance.preset.replace('_', ' ') + "   >"));
        oreButton.setMessage(label("ORE OUTLINES: " + flag(VisionModClient.oreHighlight)));
        radiusButton.setMessage(label("ORE RADIUS: " + VisionPerformance.oreRadius));
        budgetButton.setMessage(label("BLOCKS / TICK: " + VisionPerformance.checksPerTick));
        limitButton.setMessage(label("MAX OUTLINES: " + VisionPerformance.outlineLimit));
        heartsButton.setMessage(label("PLAYER HEARTS: " + flag(VisionModClient.playerHearts)));
        targetFpsButton.setMessage(label("BOOST TARGET: " + VisionRenderBoost.targetFps));
        particleButton.setMessage(label("FILTER PARTICLES: " + flag(VisionModClient.particles)));
        allParticleButton.setMessage(label("HIDE ALL: " + flag(VisionModClient.hideAllParticles)));
    }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) { }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        VisionMenuTheme.panel(g, font, width, height, "VISION / PERFORMANCE",
            "Presets, draw distance and effects");
        super.render(g, mouseX, mouseY, delta);
        // Paint the same neon button skin over the underlying vanilla buttons.
        // Their hitboxes and event handlers are unchanged.
        int x = VisionMenuTheme.x(width), y = VisionMenuTheme.y(height);
        skin(g, presetButton, mouseX, mouseY);
        skin(g, oreButton, mouseX, mouseY);
        skin(g, radiusButton, mouseX, mouseY);
        skin(g, budgetButton, mouseX, mouseY);
        skin(g, limitButton, mouseX, mouseY);
        skin(g, heartsButton, mouseX, mouseY);
        skin(g, targetFpsButton, mouseX, mouseY);
        skin(g, particleButton, mouseX, mouseY);
        skin(g, allParticleButton, mouseX, mouseY);
        VisionMenuTheme.button(g, font, x + 12, y + 204, 312, 20,
            "BACK TO VISUALS", VisionMenuTheme.inside(mouseX, mouseY, x + 12, y + 204, 312, 20));
    }

    private void skin(GuiGraphics g, Button button, int mouseX, int mouseY) {
        int x = button.getX(), y = button.getY();
        VisionMenuTheme.button(g, font, x, y, button.getWidth(), button.getHeight(),
            button.getMessage().getString(),
            VisionMenuTheme.inside(mouseX, mouseY, x, y, button.getWidth(), button.getHeight()));
    }

    @Override public void onClose() { minecraft.setScreen(new VisionDashboardScreen()); }
    @Override public boolean isPauseScreen() { return false; }
}
