package com.visionmod;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** One quiet lyric line. Browser clock → local LRC → optional manual text. */
public final class VisionWorldLyrics {
    private static String cachedText = "";
    private static String[] lines = {""};
    private VisionWorldLyrics() {}
    public static void init() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("visionmod", "world_lyrics_visible"), (g,t)->render(g));
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(VisionWorldLyrics::renderInWorld);
    }
    public static String previewLine(String text, long ticks) {
        text = text == null ? "" : text;
        if (!text.equals(cachedText)) {
            cachedText = text;
            lines = java.util.Arrays.stream(text.split("\\|"))
                .map(String::trim).filter(s->!s.isBlank()).limit(12).toArray(String[]::new);
        }
        return lines.length == 0 ? "" : lines[(int)Math.floorMod(Math.floorDiv(ticks,70L), lines.length)];
    }
    private static String current(Minecraft mc) {
        String timed = VisionLyricsSource.current();
        if (timed != null && !timed.isBlank()) return timed;
        String manual = previewLine(VisionExtendedFeatures.customText(), mc.level.getGameTime());
        return manual.isBlank() ? "♪ Нет музыки" : manual;
    }
    private static boolean ready(Minecraft mc) {
        return VisionModClient.enabled && VisionExtendedFeatures.enabled(3) && mc.player != null && mc.level != null;
    }
    private static Vec3 anchor(Minecraft mc) {
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        return mc.player.getPosition(partial).add(0,mc.player.getBbHeight()+.65,0);
    }
    private static boolean useWorld(Minecraft mc, int width, int height) {
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON ||
            VisionHudLayout.moved(VisionHudLayout.Widget.LYRICS) || VisionHudLayout.chatOpen(mc)) return false;
        var camera = mc.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) return false;
        Vec3 forward = Vec3.directionFromRotation(camera.xRot(),camera.yRot());
        Vec3 right = forward.cross(new Vec3(0,1,0));
        if (right.lengthSqr() < 1e-5) return false;
        right = right.normalize();
        Vec3 rel = anchor(mc).subtract(camera.position());
        double depth = rel.dot(forward);
        if (depth < .4 || depth > 32) return false;
        double factor = height/(2*Math.tan(Math.toRadians(Math.clamp(mc.options.fov().get(),35,110)*.5)));
        double x = width*.5+rel.dot(right)*factor/depth;
        double y = height*.5-rel.dot(right.cross(forward).normalize())*factor/depth;
        int margin = Math.min(width/2, mc.font.width(current(mc))/2+12);
        return x>margin && x<width-margin && y>24 && y<height-36;
    }
    private static void renderInWorld(WorldRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (!ready(mc) || ctx.matrices()==null || ctx.consumers()==null ||
            !useWorld(mc,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight())) return;
        Vec3 relative = anchor(mc).subtract(mc.gameRenderer.getMainCamera().position());
        String text = current(mc);
        PoseStack pose = ctx.matrices();
        pose.pushPose();
        try {
            pose.translate(relative.x,relative.y,relative.z);
            pose.mulPose(mc.gameRenderer.getMainCamera().rotation());
            pose.scale(-.022f,-.022f,.022f);
            mc.font.drawInBatch(text,-mc.font.width(text)*.5f,0,0xFFF4F1FF,true,
                pose.last().pose(),ctx.consumers(),Font.DisplayMode.NORMAL,0x700E1120,VisionGeometry.FULL_BRIGHT);
        } finally { pose.popPose(); }
    }
    private static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!ready(mc) || !VisionHudLayout.hudVisible(mc) || useWorld(mc,g.guiWidth(),g.guiHeight())) return;
        String text = current(mc);
        int max = Math.max(48,Math.min(300,g.guiWidth()-40));
        text = mc.font.plainSubstrByWidth(text,max);
        int ww = mc.font.width(text)+24;
        var b = VisionHudLayout.place(VisionHudLayout.Widget.LYRICS,g.guiWidth()-ww-16,25,ww,27);
        int x=b.x(),y=b.y();
        VisionHudTheme.panel(g,x,y,ww,27,VisionMenuTheme.CYAN);
        g.drawString(mc.font,text,x+12,y+9,0xFFF5F1FF);
        float progress = VisionLyricsSource.progress();
        if(progress>=0)g.fill(x+8,y+25,x+8+(int)((ww-16)*progress),y+26,VisionMenuTheme.CYAN);
    }
}
