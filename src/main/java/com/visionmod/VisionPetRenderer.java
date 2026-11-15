package com.visionmod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

/**
 * Original animated, solid-color miniature companion.
 *
 * A small floating pastel fox/cat with visible eyes, contrasting face,
 * ears, belly, feet and animated tail/wings. It is local/client-only and
 * does not spawn entities, send packets, or modify the player's model.
 */
public final class VisionPetRenderer {
    // If a conflicting renderer/shader throws, stop this visual rather than
    // crashing the Minecraft client on every subsequent frame.
    private static boolean renderFaulted;
    public static void retryAfterToggle(){renderFaulted=false;}
    private VisionPetRenderer() {}

    public static void init() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(ctx -> {
            if (renderFaulted || !VisionModClient.enabled || !VisionExtendedFeatures.enabled(4)
                || ctx.matrices() == null || ctx.consumers() == null) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            Vec3 cam = mc.gameRenderer.getMainCamera().position();
            double t = System.nanoTime() * 1.0e-9;
            double playerYaw = Math.toRadians(mc.player.getYRot());
            double sideX = Math.cos(playerYaw) * 0.85;
            double sideZ = Math.sin(playerYaw) * 0.85;
            // Slow, restrained hovering without rotating around the player's head.
            double bob = Math.sin(t * 2.4) * .075;
            Vec3 p = mc.player.position().add(sideX, 1.45 + bob, sideZ);
            PoseStack pose = ctx.matrices();
            pose.pushPose();
            try {
            pose.translate(p.x-cam.x,p.y-cam.y,p.z-cam.z);
            // Always show the face toward the local camera, independent of Free Look.
            pose.mulPose(Axis.YP.rotationDegrees(180f - mc.gameRenderer.getMainCamera().yRot()));
            float pitch = (float)Math.sin(t*1.7)*3.3f;
            pose.mulPose(Axis.ZP.rotationDegrees(pitch));
            // A shared ImmediateBufferSource finalizes a buffer after switching
            // texture/render type. Draw ALL fur surfaces before ALL face details,
            // then the line aura LAST. Never hold old consumers across getBuffer.
            final int fur=VisionExtendedFeatures.petColor();
            final int shadow=VisionGeometry.blendRgb(fur,0xFF655187,.36f);
            final int face=VisionGeometry.blendRgb(fur,0xFFFFFFFF,.68f);
            final int eye=0xFF21263D,shine=0xFFFFFFFF,blush=0xFFFFA7C3;
            final int belly=0xFFFEF4FF,foot=0xFFBCA0E5;
            float tail=(float)Math.sin(t*2.8)*.075f;
            float flap=(float)Math.sin(t*5.0)*.08f;
            // ONE render type for the whole pet. Switching fur→face→lines
            // used to close the shared ImmediateBufferSource and could throw
            // IllegalStateException("Not building!") mid-frame on modded renderers.
            VertexConsumer furTexture=ctx.consumers().getBuffer(
                RenderTypes.entityTranslucent(VisionGeometry.PET_FUR));
            VisionGeometry.cube(furTexture,pose,-.17f,-.24f,-.13f,.17f,.10f,.13f,shadow);
            VisionGeometry.cube(furTexture,pose,-.215f,.005f,-.19f,.215f,.315f,.19f,fur);
            VisionGeometry.cube(furTexture,pose,-.19f,.25f,-.10f,-.105f,.43f,.085f,fur);
            VisionGeometry.cube(furTexture,pose,.105f,.25f,-.10f,.19f,.43f,.085f,fur);
            VisionGeometry.cube(furTexture,pose,.10f+tail,-.16f,.11f,.26f+tail,-.095f,.33f,fur);
            VisionGeometry.cube(furTexture,pose,.235f+tail,-.10f,.295f,.325f+tail,.075f,.39f,face);

            VertexConsumer solid=furTexture;
            VisionGeometry.cube(solid,pose,-.15f,.04f,-.198f,.15f,.225f,-.187f,face);
            VisionGeometry.cube(solid,pose,-.105f,-.20f,-.142f,.105f,.015f,-.131f,belly);
            VisionGeometry.cube(solid,pose,-.16f,.30f,-.105f,-.123f,.403f,-.09f,blush);
            VisionGeometry.cube(solid,pose,.123f,.30f,-.105f,.16f,.403f,-.09f,blush);
            VisionGeometry.cube(solid,pose,-.137f,.154f,-.212f,-.085f,.21f,-.195f,eye);
            VisionGeometry.cube(solid,pose,.085f,.154f,-.212f,.137f,.21f,-.195f,eye);
            VisionGeometry.cube(solid,pose,-.124f,.188f,-.217f,-.107f,.209f,-.207f,shine);
            VisionGeometry.cube(solid,pose,.107f,.188f,-.217f,.124f,.209f,-.207f,shine);
            VisionGeometry.cube(solid,pose,-.022f,.107f,-.218f,.022f,.146f,-.199f,blush);
            VisionGeometry.cube(solid,pose,-.178f,.105f,-.201f,-.139f,.132f,-.191f,blush);
            VisionGeometry.cube(solid,pose,.139f,.105f,-.201f,.178f,.132f,-.191f,blush);
            VisionGeometry.cube(solid,pose,-.14f,-.297f,-.13f,-.034f,-.218f,-.05f,foot);
            VisionGeometry.cube(solid,pose,.034f,-.297f,-.13f,.14f,-.218f,-.05f,foot);
            VisionGeometry.cube(solid,pose,-.335f-flap,-.025f,.045f,-.175f,.145f,.095f,0xBDAED5FF);
            VisionGeometry.cube(solid,pose,.175f,-.025f,.045f,.335f+flap,.145f,.095f,0xBDAED5FF);

            // The previous outline aura changed render type and could
            // invalidate an outstanding pet consumer on Sodium/Iris.
            // Use a small filled halo with the same texture/buffer instead.
            final int segments=20;
            for(int i=0;i<segments;i++){
                double a=i*2*Math.PI/segments,b=(i+1)*2*Math.PI/segments;
                float ro=.33f,ri=.305f;
                VisionGeometry.quad(solid,pose,
                    (float)(Math.cos(a)*ro),-.335f,(float)(Math.sin(a)*ro),
                    (float)(Math.cos(a)*ri),-.335f,(float)(Math.sin(a)*ri),
                    (float)(Math.cos(b)*ri),-.335f,(float)(Math.sin(b)*ri),
                    (float)(Math.cos(b)*ro),-.335f,(float)(Math.sin(b)*ro),
                    VisionGeometry.withAlpha(fur,160),0,1,0);
            }
            } catch (RuntimeException ex) {
                renderFaulted=true;
                System.err.println("[VisionMod][PET] Disabled after render error to prevent repeated client crash: "+ex);
                ex.printStackTrace();
            } finally {
                pose.popPose();
            }
        });
    }
}
