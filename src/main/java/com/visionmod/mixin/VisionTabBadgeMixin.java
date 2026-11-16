package com.visionmod.mixin;

import com.visionmod.VisionPresenceClient;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class VisionTabBadgeMixin {
    @Inject(method="getNameForDisplay",at=@At("RETURN"),cancellable=true)
    private void visionmod$badge(PlayerInfo player,CallbackInfoReturnable<Component> cir){
        cir.setReturnValue(VisionPresenceClient.decorate(player.getProfile().id(),cir.getReturnValue()));
    }
}
