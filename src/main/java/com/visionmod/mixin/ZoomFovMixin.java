package com.visionmod.mixin;

import com.visionmod.VisionModClient;
import com.visionmod.VisionExtras;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only adjusts local camera field of view, not server-side reach. */
@Mixin(GameRenderer.class)
public abstract class ZoomFovMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void visionmod$zoom(Camera camera, float tick, boolean changing,
                               CallbackInfoReturnable<Float> cir) {
        if (VisionModClient.enabled && VisionExtras.zoom && VisionExtras.zoomHeld) {
            cir.setReturnValue(cir.getReturnValue() * VisionExtras.zoomFactor);
        }
    }
}
