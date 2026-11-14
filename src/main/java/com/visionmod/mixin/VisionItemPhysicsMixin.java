package com.visionmod.mixin;

import com.visionmod.VisionMotionEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-only optional flat/spinning dropped-item presentation.
 * Inject after the renderer's own pose push to avoid leaking transforms into
 * unrelated renders. Fails open if an alternate renderer has no matching site.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class VisionItemPhysicsMixin {
    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/CameraRenderState;)V",
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
            shift = At.Shift.AFTER,
            ordinal = 0),
        require = 0)
    private void visionmod$itemPhysics(ItemEntityRenderState state, PoseStack pose,
        SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (!VisionMotionEffects.enabled(1)) return;
        // Gently animated rotation, no physics/server mutation.
        pose.mulPose(Axis.XP.rotationDegrees(88.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(state.ageInTicks * 0.8F));
    }
}
