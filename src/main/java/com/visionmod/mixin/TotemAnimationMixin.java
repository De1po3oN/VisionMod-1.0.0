package com.visionmod.mixin;

import com.visionmod.VisionAdvanced;
import com.visionmod.VisionModClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Separate from held-totem scaling. Verify the visual result on Minecraft 1.21.11. */
@Mixin(ScreenEffectRenderer.class)
public abstract class TotemAnimationMixin {
    @Shadow private ItemStack itemActivationItem;
    @Unique private boolean visionmod$scaleApplied;

    @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), require = 0)
    private void visionmod$begin(PoseStack matrices, float partialTicks, SubmitNodeCollector collector,
                                 CallbackInfo ci) {
        visionmod$scaleApplied = VisionModClient.enabled && VisionAdvanced.smallTotemAnimation
            && itemActivationItem != null && itemActivationItem.is(Items.TOTEM_OF_UNDYING);
        if (visionmod$scaleApplied) {
            float scale = VisionAdvanced.totemAnimationPercent / 100.0f;
            matrices.pushPose();
            matrices.scale(scale, scale, scale);
        }
    }

    @Inject(method = "renderItemActivationAnimation", at = @At("RETURN"), require = 0)
    private void visionmod$end(PoseStack matrices, float partialTicks, SubmitNodeCollector collector,
                               CallbackInfo ci) {
        if (visionmod$scaleApplied) {
            matrices.popPose();
            visionmod$scaleApplied = false;
        }
    }
}
