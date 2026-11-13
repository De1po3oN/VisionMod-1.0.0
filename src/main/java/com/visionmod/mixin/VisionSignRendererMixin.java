package com.visionmod.mixin;

import com.visionmod.VisionSignOptimizer;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optimizes sign text render state without skipping or flickering render frames. */
@Mixin(AbstractSignRenderer.class)
public abstract class VisionSignRendererMixin {
    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At("TAIL"),
        require = 0
    )
    private void visionmod$throttleDistantSignText(
            SignBlockEntity sign,
            SignRenderState state,
            float tickDelta,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay breakProgress,
            CallbackInfo ci
    ) {
        VisionSignOptimizer.apply(sign, state, cameraPos);
    }
}
