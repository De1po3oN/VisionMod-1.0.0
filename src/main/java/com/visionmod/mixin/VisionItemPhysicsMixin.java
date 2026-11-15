package com.visionmod.mixin;

import com.visionmod.VisionItemPhysicsState;
import com.visionmod.VisionMotionEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders dropped items lying nearly flat at rest, softly tumbling in the air.
 * Real collision/gravity remain fully vanilla/server-authoritative.
 * All transforms are scoped to the render pose push/pop from the base renderer.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class VisionItemPhysicsMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
        at = @At("TAIL"),
        require = 1)
    private void visionmod$recordMotion(ItemEntity entity, ItemEntityRenderState state,
                                        float partialTick, CallbackInfo ci) {
        ((VisionItemPhysicsState)state).visionmod$setMotion(
            entity.onGround(),entity.getDeltaMovement().lengthSqr());
    }

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
            shift = At.Shift.AFTER,
            ordinal = 0),
        require = 1)
    private void visionmod$poseDroppedItem(ItemEntityRenderState state,PoseStack pose,
        SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo ci) {
        if(!VisionMotionEffects.enabled(1))return;
        VisionItemPhysicsState physics=(VisionItemPhysicsState)state;
        float t=state.ageInTicks;
        if(physics.visionmod$isGrounded() && physics.visionmod$speed()<.014){
            // Stable resting pose, lifted slightly off the floor to avoid Z-fighting.
            pose.translate(0,.065,0);
            pose.mulPose(Axis.XP.rotationDegrees(87.0f));
            pose.mulPose(Axis.ZP.rotationDegrees((float)Math.sin(t*.055f)*1.5f));
        }else{
            // Visually react to vertical travel; no model spin explosion.
            float movement=(float)Math.min(1,Math.sqrt(physics.visionmod$speed())*5f);
            float spin=t*(.4f+movement*2.4f);
            pose.mulPose(Axis.XP.rotationDegrees(18f+(float)Math.sin(spin*.035f)*23f));
            pose.mulPose(Axis.ZP.rotationDegrees(spin));
        }
    }
}
