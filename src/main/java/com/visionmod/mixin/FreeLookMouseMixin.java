package com.visionmod.mixin;

import com.visionmod.FreeLook;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class FreeLookMouseMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Inject(method="turnPlayer(D)V", at=@At("HEAD"), cancellable=true, require=1)
    private void visionmod$freeLook(double frameTime, CallbackInfo ci) {
        // Mouse movement may arrive between client ticks. Resync the hold
        // state here so the initial movement can never rotate the player.
        FreeLook.syncFromMouse();
        if(!FreeLook.active) return;
        FreeLook.rotate(accumulatedDX,accumulatedDY);
        accumulatedDX=0;
        accumulatedDY=0;
        ci.cancel();
    }
}
