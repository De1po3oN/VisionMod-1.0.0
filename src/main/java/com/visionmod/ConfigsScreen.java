package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Shareable JSON profiles with prominent folder shortcut. */
public final class ConfigsScreen extends Screen {
    private EditBox nameInput;
    private Button selectedButton;
    private List<String> profiles = List.of();
    private int selected = 0;
    private String status = "Save here, or copy JSON files into the folder";

    public ConfigsScreen() { super(Component.literal("Vision Mod / Configs")); }

    private void refresh() {
        String previous = profiles.isEmpty() ? null : profiles.get(Math.min(selected, profiles.size() - 1));
        profiles = VisionProfiles.list();
        selected = previous == null ? 0 : Math.max(0, profiles.indexOf(previous));
        if (selectedButton != null) selectedButton.setMessage(Component.literal("PROFILE: " + current() + "    >"));
    }

    private String current() {
        return profiles.isEmpty() ? "(none)" : profiles.get(Math.min(selected, profiles.size() - 1));
    }

    @Override
    protected void init() {
        int x = VisionMenuTheme.x(width), y = VisionMenuTheme.y(height);
        nameInput = new EditBox(font, x + 13, y + 51, 310, 20,
            Component.literal("Profile name"));
        nameInput.setMaxLength(40);
        nameInput.setHint(Component.literal("my_pvp_config"));
        addRenderableWidget(nameInput);

        selectedButton = addRenderableWidget(Button.builder(Component.literal("PROFILE: (none)"), b -> {
            refresh();
            if (!profiles.isEmpty()) selected = (selected + 1) % profiles.size();
            selectedButton.setMessage(Component.literal("PROFILE: " + current() + "    >"));
        }).bounds(x + 13, y + 77, 310, 20).build());

        addRenderableWidget(Button.builder(Component.literal("SAVE CURRENT"), b -> {
            try {
                VisionProfiles.save(nameInput.getValue().trim());
                status = "Saved: " + nameInput.getValue().trim();
                refresh();
            } catch (Exception e) { status = "Save failed: " + e.getMessage(); }
        }).bounds(x + 13, y + 103, 152, 20).build());
        addRenderableWidget(Button.builder(Component.literal("LOAD PROFILE"), b -> {
            try {
                VisionProfiles.load(current());
                status = "Loaded: " + current();
            } catch (Exception e) { status = "Load failed: " + e.getMessage(); }
        }).bounds(x + 171, y + 103, 152, 20).build());

        addRenderableWidget(Button.builder(Component.literal("DELETE PROFILE"), b -> {
            try {
                VisionProfiles.delete(current());
                status = "Profile deleted";
                refresh();
            } catch (Exception e) { status = "Delete failed: " + e.getMessage(); }
        }).bounds(x + 13, y + 129, 152, 20).build());
        addRenderableWidget(Button.builder(Component.literal("REFRESH LIST"), b -> {
            refresh();
            status = "Profile list refreshed";
        }).bounds(x + 171, y + 129, 152, 20).build());

        addRenderableWidget(Button.builder(Component.literal("OPEN CONFIGS FOLDER"), b -> {
            status = VisionProfiles.openFolder() ?
                "Folder opened - drop JSON configs here" : "Folder: config/visionmod/profiles";
            refresh();
        }).bounds(x + 13, y + 158, 310, 26).build());

        addRenderableWidget(Button.builder(Component.literal("BACK TO VISUALS"),
            b -> minecraft.setScreen(new VisionDashboardScreen()))
            .bounds(x + 13, y + 204, 310, 20).build());
        refresh();
    }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) { }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        VisionMenuTheme.panel(g, font, width, height, "VISION / CONFIGS",
            "Save, share, load and import profiles");
        super.render(g, mouseX, mouseY, delta);
        int x = VisionMenuTheme.x(width), y = VisionMenuTheme.y(height);
        String shown = status;
        // Keep messages inside the panel even on an unsuccessful filesystem operation.
        while (shown.length() > 0 && font.width(shown) > 309) {
            shown = shown.substring(0, shown.length() - 1);
        }
        // Uniform visual skin for actions, while retaining native focus/click handling.
        skin(g, mouseX, mouseY, x + 13, y + 77, 310, 20, "PROFILE: " + current() + "    >");
        skin(g, mouseX, mouseY, x + 13, y + 103, 152, 20, "SAVE CURRENT");
        skin(g, mouseX, mouseY, x + 171, y + 103, 152, 20, "LOAD PROFILE");
        skin(g, mouseX, mouseY, x + 13, y + 129, 152, 20, "DELETE PROFILE");
        skin(g, mouseX, mouseY, x + 171, y + 129, 152, 20, "REFRESH LIST");
        skin(g, mouseX, mouseY, x + 13, y + 158, 310, 26, "OPEN CONFIGS FOLDER");
        skin(g, mouseX, mouseY, x + 13, y + 204, 310, 20, "BACK TO VISUALS");
        g.drawCenteredString(font, shown, x + 168, y + 190, VisionMenuTheme.CYAN);
    }

    private void skin(GuiGraphics g, int mouseX, int mouseY, int x, int y, int w, int h,
                      String title) {
        VisionMenuTheme.button(g, font, x, y, w, h, title,
            VisionMenuTheme.inside(mouseX, mouseY, x, y, w, h));
    }

    @Override public void onClose() { minecraft.setScreen(new VisionDashboardScreen()); }
    @Override public boolean isPauseScreen() { return false; }
}
