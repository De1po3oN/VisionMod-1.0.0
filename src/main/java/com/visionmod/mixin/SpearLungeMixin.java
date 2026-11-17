package com.visionmod.mixin;

import com.visionmod.VisionMotionEffects;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe the native client stab action, including its deferred lunge movement. */
@Mixin(MultiPlayerGameMode.class)
public abstract class SpearLungeMixin {
    @Inject(method="piercingAttack", at=@At("HEAD"))
    private void visionmod$beforeLunge(CallbackInfo ci) {
        VisionMotionEffects.beforeLunge();
    }
    @Inject(method="piercingAttack", at=@At("TAIL"))
    private void visionmod$afterLunge(CallbackInfo ci) {
        VisionMotionEffects.afterLunge();
    }
}
