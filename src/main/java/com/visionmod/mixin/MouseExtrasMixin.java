package com.visionmod.mixin;

import com.visionmod.VisionExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gameplay click counter. Inventory scrolling is handled by VisionFastScroller. */
@Mixin(MouseHandler.class)
public abstract class MouseExtrasMixin {
    @Inject(method = "onButton", at = @At("HEAD"), require = 0)
    private void visionmod$countClick(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (action == GLFW.GLFW_PRESS && mc.screen == null && mc.player != null) VisionExtras.clicked();
    }

}
