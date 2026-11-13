package com.visionmod.mixin;

import com.visionmod.VisionExtras;
import com.visionmod.VisionModClient;
import com.visionmod.VisionSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reacts only to vanilla arrow-hit-player sound packets delivered to this client. */
@Mixin(ClientPacketListener.class)
public abstract class ArrowHitSoundMixin {
    @Inject(method = "handleSoundEvent", at = @At("HEAD"), require = 0)
    private void visionmod$arrowHitSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (!VisionModClient.enabled || !VisionExtras.arrowHitSound) return;
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread() || mc.player == null || mc.level == null) return;
        if (!"minecraft:entity.arrow.hit_player".equals(packet.getSound().value().location().toString())) return;
        // This packet does not identify the shooter, so other nearby arrow impacts
        // can also trigger the clip. This limitation is displayed in the README.
        mc.getSoundManager().play(SimpleSoundInstance.forUI(VisionSounds.ARROW_HIT, 1.0F));
    }
}
