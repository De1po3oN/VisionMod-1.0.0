package com.visionmod;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Draw only the outline SHAPE of an existing, targeted block, never a floating full cube. */
public final class TargetOutline {
    private TargetOutline() { }

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!VisionModClient.enabled || !VisionExtras.targetOutline ||
                    context.consumers() == null || context.matrices() == null) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null ||
                    !(mc.hitResult instanceof BlockHitResult hit) ||
                    hit.getType() != HitResult.Type.BLOCK) return;

            var blockPos = hit.getBlockPos();
            if (!mc.level.hasChunkAt(blockPos)) return;
            BlockState state = mc.level.getBlockState(blockPos);
            if (state.isAir()) return; // The client may still have an old hit after a block breaks.
            VoxelShape shape = state.getShape(mc.level, blockPos);
            if (shape.isEmpty()) return;

            Vec3 camera = mc.gameRenderer.getMainCamera().position();
            VertexConsumer lines = context.consumers().getBuffer(RenderTypes.lines());
            ShapeRenderer.renderShape(context.matrices(), lines, shape,
                    blockPos.getX() - camera.x,
                    blockPos.getY() - camera.y,
                    blockPos.getZ() - camera.z,
                    VisionExtras.outlineColor, 1.7F);
        });
    }
}
