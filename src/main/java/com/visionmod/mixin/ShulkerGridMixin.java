package com.visionmod.mixin;

import com.visionmod.VisionAdvanced;
import com.visionmod.VisionModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/** 9x3 shulker preview, shown only when hovering an item in a container screen. */
@Mixin(AbstractContainerScreen.class)
public abstract class ShulkerGridMixin {
    @Shadow protected Slot hoveredSlot;

    @Inject(method = "renderTooltip", at = @At("TAIL"), require = 0)
    private void visionmod$shulkerGrid(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!VisionModClient.enabled || !VisionAdvanced.shulkerGrid || hoveredSlot == null) return;
        ItemStack box = hoveredSlot.getItem();
        if (!(box.getItem() instanceof BlockItem block) || !(block.getBlock() instanceof ShulkerBoxBlock)) return;
        ItemContainerContents contents = box.get(DataComponents.CONTAINER);
        if (contents == null) return;
        NonNullList<ItemStack> slots = NonNullList.withSize(27, ItemStack.EMPTY);
        try {
            contents.copyInto(slots); // Preserves empty cells and their positions.
        } catch (RuntimeException malformedItem) {
            return; // A malformed third-party shulker must never crash the inventory screen.
        }
        int w = 9*18 + 12, h = 3*18 + 26;
        int x = mouseX + 17;
        int y = mouseY - h / 2;
        if (x + w > graphics.guiWidth()) x = mouseX - w - 17;
        x = Math.max(4, Math.min(x, graphics.guiWidth() - w - 4));
        y = Math.max(4, Math.min(y, graphics.guiHeight() - h - 4));
        graphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF4B7790);
        graphics.fill(x, y, x + w, y + h, 0xFA142033);
        graphics.drawString(Minecraft.getInstance().font, "SHULKER  9x3", x + 7, y + 6, 0xFF67E8F9);
        for (int i = 0; i < 27; i++) {
            int bx = x + 6 + (i % 9) * 18, by = y + 19 + (i / 9) * 18;
            graphics.fill(bx, by, bx + 17, by + 17, 0xFF26364D);
            ItemStack item = slots.get(i);
            if (!item.isEmpty()) {
                graphics.renderItem(item, bx, by);
                graphics.renderItemDecorations(Minecraft.getInstance().font, item, bx, by);
            }
        }
    }
}
