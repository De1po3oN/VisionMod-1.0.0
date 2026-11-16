package com.visionmod;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** Camera-facing, two-dimensional lyrics in the world, including first person. */
public final class VisionWorldLyrics {
    private static String cachedText = "";
    private static String[] lines = {""};
    private static long renderedFrames;
    private static Vec3 lastAnchor;
    private static String lastRenderedText="";
    private VisionWorldLyrics() {}
    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(VisionWorldLyrics::renderInWorld);
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
        return timed != null ? timed : previewLine(VisionExtendedFeatures.customText(), mc.level.getGameTime());
    }
    private static void renderInWorld(WorldRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (!VisionModClient.enabled || !VisionExtendedFeatures.enabled(3) || mc.player == null ||
            mc.level == null || ctx.matrices()==null || ctx.consumers()==null) return;
        var camera=mc.gameRenderer.getMainCamera();
        if(!camera.isInitialized())return;
        String text=current(mc);
        if(text.isBlank())return;
        // Camera's own axes also work looking straight up/down and in front-facing F5.
        var offset=new org.joml.Vector3f(0,-.40f,-2.4f).rotate(camera.rotation());
        Vec3 relative=new Vec3(offset.x,offset.y,offset.z);
        lastAnchor=camera.position().add(relative);
        float scale=.014f;
        double aspect=(double)mc.getWindow().getWidth()/Math.max(1,mc.getWindow().getHeight());
        int width=(int)Math.clamp(2*2.4*Math.tan(Math.toRadians(Math.clamp(mc.options.fov().get(),30,110)*.5))*aspect*.70/scale,70,240);
        var rows=mc.font.split(Component.literal(text),width);
        PoseStack pose=ctx.matrices();pose.pushPose();
        try {
            pose.translate(relative.x,relative.y,relative.z);
            pose.mulPose(camera.rotation());
            pose.scale(-scale,-scale,scale);
            // One translucent text layer; no floating box, HUD copy or stale transition line.
            int count=Math.min(3,rows.size());
            for(int i=0;i<count;i++) {
                var row=rows.get(i);
                mc.font.drawInBatch(row,-mc.font.width(row)*.5f,i*11,0xFFF4F1FF,true,
                    pose.last().pose(),ctx.consumers(),Font.DisplayMode.SEE_THROUGH,0,VisionGeometry.FULL_BRIGHT);
            }
            renderedFrames++;
            lastRenderedText=text;
        } finally {pose.popPose();}
    }
    static long renderedFrames(){return renderedFrames;}
    static Vec3 lastAnchor(){return lastAnchor;}
    static String lastRenderedText(){return lastRenderedText;}
}
