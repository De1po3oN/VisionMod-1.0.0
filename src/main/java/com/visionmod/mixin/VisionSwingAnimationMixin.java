package com.visionmod.mixin;

import com.visionmod.VisionMotionEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-only hand-swing offset; vanilla swing and item interactions still run.
 * The hook is isolated from server mechanics and off by default.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class VisionSwingAnimationMixin {
    @Inject(
        method = "applyItemArmAttackTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
        at = @At("HEAD"), require = 0)
    private void visionmod$customSwing(PoseStack pose, HumanoidArm arm, float progress,
        CallbackInfo ci) {
        if (!VisionMotionEffects.enabled(0)) return;
        float amount = Mth.sin(progress * (float)Math.PI);
        float side = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
        pose.translate(side * amount * 0.07f, -amount * .035f, -amount * .12f);
        pose.mulPose(Axis.ZP.rotationDegrees(side * amount * 18.0f));
        pose.mulPose(Axis.XP.rotationDegrees(-amount * 8.0f));
    }
}
