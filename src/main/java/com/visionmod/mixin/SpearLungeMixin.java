package com.visionmod.mixin;

import com.visionmod.VisionMotionEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe vanilla's lunge impulse; never modify movement or enchantment behavior. */
@Mixin(Player.class)
public abstract class SpearLungeMixin {
    @Inject(method="lungeForwardMaybe", at=@At("HEAD"))
    private void visionmod$beforeLunge(CallbackInfo ci) {
        if ((Object)this == Minecraft.getInstance().player) VisionMotionEffects.beforeLunge();
    }
    @Inject(method="lungeForwardMaybe", at=@At("TAIL"))
    private void visionmod$afterLunge(CallbackInfo ci) {
        if ((Object)this == Minecraft.getInstance().player) VisionMotionEffects.afterLunge();
    }
}
