package com.visionmod.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Use vanilla's slot hit test, including active-slot rules. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
    @Invoker("getSlotAt") Slot visionmod$slotAt(double x,double y);
    @Accessor("leftPos") int visionmod$left();
    @Accessor("topPos") int visionmod$top();
}
