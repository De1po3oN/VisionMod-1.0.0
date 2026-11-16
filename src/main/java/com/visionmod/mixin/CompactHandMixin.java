package com.visionmod.mixin;

import com.visionmod.VisionExtras;
import com.visionmod.VisionModClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.tags.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional first-person held-item shrink. Uses balanced push/pop per render invocation. */
@Mixin(ItemInHandRenderer.class)
public abstract class CompactHandMixin {
    private static boolean shouldShrink(ItemStack stack) {
        if (!VisionModClient.enabled || stack.isEmpty()) return false;
        if (VisionExtras.smallSword && stack.is(ItemTags.SWORDS)) return true;
        if (VisionExtras.smallShield && stack.getItem() instanceof ShieldItem) return true;
        return VisionExtras.smallTotem && stack.is(Items.TOTEM_OF_UNDYING);
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), require = 0)
    private void visionmod$compactHandStart(AbstractClientPlayer player, float partial, float pitch,
            InteractionHand hand, float swing, ItemStack stack, float equip, PoseStack pose,
            SubmitNodeCollector queue, int light, CallbackInfo ci) {
        if (!shouldShrink(stack)) return;
        pose.pushPose();
        pose.translate(0.0f, -0.10f, 0.0f);
        float scale=com.visionmod.VisionAppearance.handScale;
        pose.scale(scale,scale,scale);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"), require = 0)
    private void visionmod$compactHandEnd(AbstractClientPlayer player, float partial, float pitch,
            InteractionHand hand, float swing, ItemStack stack, float equip, PoseStack pose,
            SubmitNodeCollector queue, int light, CallbackInfo ci) {
        if (shouldShrink(stack)) pose.popPose();
    }
}
