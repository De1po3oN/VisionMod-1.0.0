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
    private VisionPetRenderer() {}

    public static void init() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(ctx -> {
            if (!VisionModClient.enabled || !VisionExtendedFeatures.enabled(4)
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
            pose.translate(p.x-cam.x,p.y-cam.y,p.z-cam.z);
            // Always show the face toward the local camera, independent of Free Look.
            pose.mulPose(Axis.YP.rotationDegrees(180f - mc.gameRenderer.getMainCamera().yRot()));
            float pitch = (float)Math.sin(t*1.7)*3.3f;
            pose.mulPose(Axis.ZP.rotationDegrees(pitch));
            VertexConsumer solid = ctx.consumers().getBuffer(
                RenderTypes.entityTranslucent(VisionGeometry.WHITE_TEXTURE));

            final int fur=VisionExtendedFeatures.petColor();
            final int shadow=VisionGeometry.blendRgb(fur,0xFF655187,.36f);
            final int face=VisionGeometry.blendRgb(fur,0xFFFFFFFF,.68f);
            final int eye=0xFF21263D, shine=0xFFFFFFFF, blush=0xFFFFA7C3;
            final int belly=0xFFFEF4FF, foot=0xFFBCA0E5;
            // Body, head, cheeks; no outlines or disconnected AABB dots.
            VisionGeometry.cube(solid,pose,-.17f,-.24f,-.13f,.17f,.10f,.13f,shadow);
            VisionGeometry.cube(solid,pose,-.215f,.005f,-.19f,.215f,.315f,.19f,fur);
            VisionGeometry.cube(solid,pose,-.15f,.04f,-.198f,.15f,.225f,-.187f,face);
            VisionGeometry.cube(solid,pose,-.105f,-.20f,-.142f,.105f,.015f,-.131f,belly);
            // Cute ears and small contrasting inner ears.
            VisionGeometry.cube(solid,pose,-.19f,.25f,-.10f,-.105f,.43f,.085f,fur);
            VisionGeometry.cube(solid,pose,.105f,.25f,-.10f,.19f,.43f,.085f,fur);
            VisionGeometry.cube(solid,pose,-.16f,.30f,-.105f,-.123f,.403f,-.09f,blush);
            VisionGeometry.cube(solid,pose,.123f,.30f,-.105f,.16f,.403f,-.09f,blush);
            // High-contrast readable face at 1–3 blocks.
            VisionGeometry.cube(solid,pose,-.137f,.154f,-.212f,-.085f,.21f,-.195f,eye);
            VisionGeometry.cube(solid,pose,.085f,.154f,-.212f,.137f,.21f,-.195f,eye);
            VisionGeometry.cube(solid,pose,-.124f,.188f,-.217f,-.107f,.209f,-.207f,shine);
            VisionGeometry.cube(solid,pose,.107f,.188f,-.217f,.124f,.209f,-.207f,shine);
            VisionGeometry.cube(solid,pose,-.022f,.107f,-.218f,.022f,.146f,-.199f,blush);
            VisionGeometry.cube(solid,pose,-.178f,.105f,-.201f,-.139f,.132f,-.191f,blush);
            VisionGeometry.cube(solid,pose,.139f,.105f,-.201f,.178f,.132f,-.191f,blush);
            // Paws and subtly animated tail.
            VisionGeometry.cube(solid,pose,-.14f,-.297f,-.13f,-.034f,-.218f,-.05f,foot);
            VisionGeometry.cube(solid,pose,.034f,-.297f,-.13f,.14f,-.218f,-.05f,foot);
            float tail=(float)Math.sin(t*2.8)*.075f;
            VisionGeometry.cube(solid,pose,.10f+tail,-.16f,.11f,.26f+tail,-.095f,.33f,fur);
            VisionGeometry.cube(solid,pose,.235f+tail,-.10f,.295f,.325f+tail,.075f,.39f,face);
            // Floating translucent wings with a gentle flapping animation.
            float flap=(float)Math.sin(t*5.0)*.08f;
            VisionGeometry.cube(solid,pose,-.335f-flap,-.025f,.045f,-.175f,.145f,.095f,0xBDAED5FF);
            VisionGeometry.cube(solid,pose,.175f,-.025f,.045f,.335f+flap,.145f,.095f,0xBDAED5FF);

            // Small continuous aura ring under the pet, not a 3D wireframe body.
            VertexConsumer lines = ctx.consumers().getBuffer(RenderTypes.lines());
            int count=24;
            for(int i=0;i<count;i++) {
                double a=i*Math.PI*2/count,b=(i+1)*Math.PI*2/count;
                VisionGeometry.line(lines,pose,
                    .32*Math.cos(a),-.34,.32*Math.sin(a),
                    .32*Math.cos(b),-.34,.32*Math.sin(b),
                    VisionGeometry.withAlpha(fur,170),1.45f);
            }
            pose.popPose();
        });
    }
}
