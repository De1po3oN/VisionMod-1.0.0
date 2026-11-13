package com.visionmod.mixin;

import com.visionmod.FreeLook;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Override the camera rotation BEFORE vanilla computes the third-person
 * distance and wall collision. TAIL injection changed the view direction
 * only, leaving the camera at the wrong position in space.
 */
@Mixin(Camera.class)
public abstract class FreeLookCameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
        ordinal = 0,
        shift = At.Shift.AFTER
    ))
    private void visionmod$orbit(Level area, Entity entity, boolean thirdPerson,
                                 boolean reverse, float partialTick, CallbackInfo ci) {
        if (FreeLook.isOrbitCamera() && thirdPerson && !reverse) {
            setRotation(FreeLook.yaw, FreeLook.pitch);
        }
    }
}
