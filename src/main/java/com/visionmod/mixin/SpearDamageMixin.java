package com.visionmod.mixin;

import com.visionmod.VisionMotionEffects;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class SpearDamageMixin {
    @Inject(method="handleDamageEvent", at=@At("TAIL"))
    private void visionmod$spearContact(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        VisionMotionEffects.spearDamage(packet);
    }
}
