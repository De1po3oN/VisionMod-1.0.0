package com.visionmod.mixin;

import com.visionmod.VisionExtras;
import com.visionmod.VisionModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Input counters and opt-in accelerated hotbar scrolling. */
@Mixin(MouseHandler.class)
public abstract class MouseExtrasMixin {
    @Inject(method = "onButton", at = @At("HEAD"), require = 0)
    private void visionmod$countClick(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (action == GLFW.GLFW_PRESS && mc.screen == null && mc.player != null) VisionExtras.clicked();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 0)
    private void visionmod$fastScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!VisionModClient.enabled || !VisionExtras.fastScroll || mc.player == null ||
            mc.screen != null || vertical == 0 || !mc.player.isShiftKeyDown()) return;
        int direction = vertical > 0 ? -1 : 1;
        var inventory = mc.player.getInventory();
        inventory.setSelectedSlot(Math.floorMod(inventory.getSelectedSlot() + 3 * direction, 9));
        ci.cancel();
    }
}
