package com.visionmod.mixin;

import com.visionmod.VisionExtras;
import com.visionmod.VisionModClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional low first-person fire overlay.
 *
 * Minecraft 1.21.11: renderFire(PoseStack, MultiBufferSource, TextureAtlasSprite).
 * The previous Minecraft/PoseStack callback descriptor caused a fatal
 * InvalidInjectionException even while the low-fire toggle was disabled.
 * Both handlers use the exact target argument list and balance push/pop.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class LowFireMixin {
    @Inject(method = "renderFire", at = @At("HEAD"), require = 0)
    private static void visionmod$fireBegin(PoseStack pose, MultiBufferSource bufferSource,
                                            TextureAtlasSprite sprite, CallbackInfo ci) {
        if (VisionModClient.enabled && VisionExtras.lowFire) {
            pose.pushPose();
            pose.translate(0.0f, -0.12f, 0.0f);
            pose.scale(1.0f, 0.72f, 1.0f);
        }
    }

    @Inject(method = "renderFire", at = @At("RETURN"), require = 0)
    private static void visionmod$fireEnd(PoseStack pose, MultiBufferSource bufferSource,
                                          TextureAtlasSprite sprite, CallbackInfo ci) {
        if (VisionModClient.enabled && VisionExtras.lowFire) {
            pose.popPose();
        }
    }
}
