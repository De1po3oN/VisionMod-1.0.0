package com.visionmod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** A two-sided world plane following the player, with perspective and terrain occlusion. */
public final class VisionWorldLyrics {
    private static String cachedText = "";
    private static String[] lines = {""};
    private static long renderedFrames;
    private static Vec3 lastAnchor;
    private static String lastRenderedText="";
    private static final WorldLyricMotion MOTION=new WorldLyricMotion();
    private static LocalPlayer trackedPlayer;
    private static ClientLevel trackedWorld;
    private static long previousFrame;
    private static WorldLyricMotion.Pose lastPose;
    private static final float SCALE=.020f;
    // Text render types are not all flushed after entity geometry in 1.21.11.
    // Own the font batch rather than relying on that shared, selective flush.
    private static final MultiBufferSource.BufferSource TEXT_BUFFERS=MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.ByteBufferBuilder(8192));
    private VisionWorldLyrics() {}
    public static void init() {
        WorldRenderEvents.END_MAIN.register(VisionWorldLyrics::renderInWorld);
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
            mc.level == null){MOTION.reset();previousFrame=0;lastPose=null;return;}
        if(ctx.matrices()==null)return;
        var camera=mc.gameRenderer.getMainCamera();
        if(!camera.isInitialized())return;
        if(trackedPlayer!=mc.player||trackedWorld!=mc.level){
            trackedPlayer=mc.player;trackedWorld=mc.level;MOTION.reset();previousFrame=0;
        }
        long now=System.nanoTime();double elapsed=previousFrame==0?0:(now-previousFrame)/1_000_000_000.0;previousFrame=now;
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 owner=mc.player.getPosition(partial);
        lastPose=MOTION.sample(owner.x,owner.y,owner.z,mc.player.getViewYRot(partial),elapsed,VisionAppearance.animations);
        if(lastPose==null)return;
        lastAnchor=new Vec3(lastPose.x(),lastPose.y(),lastPose.z());
        String text=current(mc);
        if(text.isBlank())return;
        Vec3 relative=lastAnchor.subtract(camera.position());
        if(relative.lengthSqr()<.65*.65)return;
        // Physical width and orientation belong to the model, not to the viewport.
        var rows=mc.font.split(Component.literal(text),160);
        Vec3 forward=Vec3.directionFromRotation(0,lastPose.yaw());
        boolean back=camera.position().subtract(lastAnchor).dot(forward)>0;
        PoseStack pose=ctx.matrices();pose.pushPose();
        try {
            pose.translate(relative.x,relative.y,relative.z);
            // Switch front/back faces of a vertical card; never billboard toward the camera.
            pose.mulPose(Axis.YP.rotationDegrees(180-lastPose.yaw()+(back?180:0)));
            pose.scale(SCALE,-SCALE,SCALE);
            int count=Math.min(3,rows.size());
            for(int i=0;i<count;i++) {
                var row=rows.get(i);
                mc.font.drawInBatch(row,-mc.font.width(row)*.5f,i*11,0xFFF4F1FF,true,
                    pose.last().pose(),TEXT_BUFFERS,Font.DisplayMode.NORMAL,0,VisionGeometry.FULL_BRIGHT);
            }
            TEXT_BUFFERS.endBatch();
            renderedFrames++;
            lastRenderedText=text;
        } finally {pose.popPose();}
    }
    static long renderedFrames(){return renderedFrames;}
    static Vec3 lastAnchor(){return lastAnchor;}
    static WorldLyricMotion.Pose lastPose(){return lastPose;}
    static String lastRenderedText(){return lastRenderedText;}
}
