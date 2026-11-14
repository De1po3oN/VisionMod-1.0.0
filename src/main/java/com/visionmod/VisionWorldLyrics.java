package com.visionmod;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Original, functional text-in-world lyrics presentation.
 *
 * Player-provided lines use | as delimiter. Lines rotate every ~3.5 seconds.
 * In third person the panel is projected above the local player; in first
 * person it hovers in front of the camera. If the projection is off-screen
 * it falls back to a safe visible screen location rather than disappearing.
 *
 * This displays player-entered text; it is NOT automatic Spotify/YouTube
 * integration and uses no copyrighted preset lyrics or third-party shaders.
 */
public final class VisionWorldLyrics {
    private static String cachedText="";
    private static String[] lines={ "YOUR WORLD · YOUR STORY" };
    private VisionWorldLyrics() {}

    public static void init() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(
            "visionmod","world_lyrics_visible"),(g,tick)->render(g));
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(VisionWorldLyrics::renderInWorld);
    }

    private static void updateLines(String text) {
        if(text.equals(cachedText))return;
        cachedText=text;
        if(text.isBlank()) {
            lines=new String[]{"YOUR WORLD · YOUR STORY"};
            return;
        }
        String[] split=text.split("\\|",-1);
        java.util.ArrayList<String> valid=new java.util.ArrayList<>(Math.min(split.length,12));
        for(String part:split) {
            String l=part.trim();
            if(!l.isEmpty())valid.add(l);
            if(valid.size()>=12)break;
        }
        lines=valid.isEmpty()?new String[]{"YOUR WORLD · YOUR STORY"}
            :valid.toArray(new String[0]);
    }

    /**
     * Real billboard text in the 3D scene with depth testing in third person,
     * while the HUD retains a visibility fallback when the anchor is offscreen.
     */
    private static void renderInWorld(WorldRenderContext ctx) {
        Minecraft mc=Minecraft.getInstance();
        if(!VisionModClient.enabled || !VisionExtendedFeatures.enabled(3) ||
            mc.level==null || mc.player==null ||
            mc.options.getCameraType()==CameraType.FIRST_PERSON ||
            ctx.matrices()==null || ctx.consumers()==null)return;
        Vec3 camera=mc.gameRenderer.getMainCamera().position();
        Vec3 anchor=worldAnchor(mc);
        if(anchor.distanceToSqr(camera)>32*32)return;
        updateLines(VisionExtendedFeatures.customText());
        String current=lines[(int)((mc.level.getGameTime()/70L)%lines.length)];
        if(current.isBlank())return;
        PoseStack pose=ctx.matrices();
        pose.pushPose();
        pose.translate(anchor.x-camera.x,anchor.y-camera.y,anchor.z-camera.z);
        pose.mulPose(mc.gameRenderer.getMainCamera().rotation());
        pose.scale(-.025f,-.025f,.025f);
        float x=-mc.font.width(current)*.5f;
        mc.font.drawInBatch(current,x,0,0xFFF8F3FF,true,
            pose.last().pose(),ctx.consumers(),Font.DisplayMode.NORMAL,
            0x550F1328,VisionGeometry.FULL_BRIGHT);
        pose.popPose();
    }

    private static Vec3 worldAnchor(Minecraft mc) {
        return mc.player.position().add(0,
            mc.player.getBbHeight()+.68+
            Math.sin(mc.level.getGameTime()*.075)*.06,0);
    }

    private static boolean anchorVisible(Minecraft mc,int w,int h) {
        var cam=mc.gameRenderer.getMainCamera();
        Vec3 forward=Vec3.directionFromRotation(cam.xRot(),cam.yRot());
        Vec3 right=forward.cross(new Vec3(0,1,0));
        if(right.lengthSqr()<1e-5)return false;
        right=right.normalize();
        Vec3 up=right.cross(forward).normalize();
        Vec3 rel=worldAnchor(mc).subtract(cam.position());
        double d=rel.dot(forward);
        if(d<.4 || d>32)return false;
        double factor=h/(2.0*Math.tan(Math.toRadians(
            Math.max(35,Math.min(110,mc.options.fov().get()))*.5)));
        int x=(int)(w*.5+rel.dot(right)*factor/d);
        int y=(int)(h*.5-rel.dot(up)*factor/d);
        return x>30 && x<w-30 && y>26 && y<h-36;
    }

    private static void render(GuiGraphics g) {
        Minecraft mc=Minecraft.getInstance();
        if(!VisionModClient.enabled || !VisionExtendedFeatures.enabled(3) ||
            mc.player==null || mc.level==null || mc.screen!=null)return;
        updateLines(VisionExtendedFeatures.customText());
        int w=g.guiWidth(),h=g.guiHeight();
        if(mc.options.getCameraType()!=CameraType.FIRST_PERSON &&
            mc.gameRenderer.getMainCamera().isInitialized() &&
            anchorVisible(mc,w,h))return;
        int phase=(int)((mc.level.getGameTime()/70L)%lines.length);
        String current=lines[phase];
        int maxTextWidth=Math.max(68,Math.min(280,w-72));
        while(!current.isEmpty() && mc.font.width(current)>maxTextWidth)
            current=current.substring(0,current.length()-1);
        if(current.isEmpty())return;

        // Stable anchor: screen projection of player position in third-person;
        // first-person text floats in forward view space.
        int sx=w/2,sy=Math.min(h-49,Math.max(38,h/2-60));
        var camera=mc.gameRenderer.getMainCamera();
        if(camera.isInitialized()) {
            Vec3 forward=Vec3.directionFromRotation(camera.xRot(),camera.yRot());
            Vec3 right=forward.cross(new Vec3(0,1,0));
            if(right.lengthSqr()>1.0e-5) {
                right=right.normalize();
                Vec3 up=right.cross(forward).normalize();
                Vec3 anchor=mc.options.getCameraType()==CameraType.FIRST_PERSON
                    ? camera.position().add(forward.scale(2.8)).add(up.scale(.26))
                    : mc.player.position().add(0,
                        mc.player.getBbHeight()+.66+Math.sin(mc.level.getGameTime()*.075)*.06,0);
                Vec3 relative=anchor.subtract(camera.position());
                double depth=relative.dot(forward);
                if(depth>.30 && depth<48) {
                    double fov=Math.max(35,Math.min(110,mc.options.fov().get()));
                    double factor=h/(2.0*Math.tan(Math.toRadians(fov*.5)));
                    int x=(int)Math.round(w*.5+relative.dot(right)*factor/depth);
                    int y=(int)Math.round(h*.5-relative.dot(up)*factor/depth);
                    int margin=mc.font.width(current)/2+14;
                    if(x>margin && x<w-margin && y>26 && y<h-34) {
                        sx=x;sy=y;
                    }
                }
            }
        }

        int width=mc.font.width(current);
        int boxW=width+32;
        int left=sx-boxW/2, top=sy-21;
        int opacity=(int)(180+40*Math.sin(mc.level.getGameTime()*.1));
        // Thin layered glow and a glassy panel, with fixed vertex count.
        g.fill(left-3,top-3,left+boxW+3,top+29,0x29118CE2);
        g.fillGradient(left,top,left+boxW,top+26,0xDC16192B,0xE0181B32);
        g.fill(left+8,top+6,left+11,top+20,VisionGeometry.withAlpha(VisionMenuTheme.CYAN,opacity));
        g.fill(left+boxW-11,top+6,left+boxW-8,top+20,
            VisionGeometry.withAlpha(VisionMenuTheme.PURPLE,opacity));
        g.drawCenteredString(mc.font,current,sx,top+8,0xFFF8F5FF);
        // Narrow progress bar for the active line.
        float progress=(mc.level.getGameTime()%70L)/70f;
        g.fill(left+11,top+23,left+boxW-11,top+25,0xFF43445C);
        g.fill(left+11,top+23,left+11+(int)((boxW-22)*progress),top+25,
            VisionMenuTheme.CYAN);
        if(lines.length>1) {
            String n=(phase+1)+"/"+lines.length;
            g.drawString(mc.font,n,left+boxW-mc.font.width(n)-2,top-11,0xFFAFA7CE);
        }
    }
}
