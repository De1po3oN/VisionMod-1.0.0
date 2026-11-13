package com.visionmod.mixin;

import com.visionmod.VisionSignOptimizer;
import com.visionmod.VisionModClient;
import com.visionmod.VisionPerformance;
import com.visionmod.VisionRenderBoost;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
    // Mojang mappings for 1.21.11: AbstractSignRenderer.isOutlineVisible(BlockPos).
    // Distant glowing text still draws normally, but omits the extra outline
    // passes while the renderer booster or adaptive FPS protection is active.
    @Inject(
        method = "isOutlineVisible(Lnet/minecraft/core/BlockPos;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void visionmod$limitDistantGlowOutline(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!VisionModClient.enabled || !(VisionRenderBoost.enabled || VisionPerformance.busy())) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player != null && player.distanceToSqr(
                pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5
        ) > 12.0 * 12.0) {
            cir.setReturnValue(false);
        }
    }
}
