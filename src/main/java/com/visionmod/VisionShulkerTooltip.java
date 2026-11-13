package com.visionmod;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** Textual inventory preview in the actual item tooltip, not only a hotbar HUD. */
public final class VisionShulkerTooltip {
    private VisionShulkerTooltip() {}

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!VisionModClient.enabled || !VisionExtras.shulkerTooltip
                || !(stack.getItem() instanceof BlockItem block)
                || !(block.getBlock() instanceof ShulkerBoxBlock)) return;
            ItemContainerContents container = stack.get(DataComponents.CONTAINER);
            if (container == null) {
                lines.add(Component.literal("Vision: Shulker is empty").withStyle(ChatFormatting.GRAY));
                return;
            }
            int count = 0, items = 0;
            java.util.ArrayList<String> first = new java.util.ArrayList<>();
            for (ItemStack inside : container.nonEmptyItems()) {
                count++;
                items += inside.getCount();
                if (first.size() < 12) first.add(inside.getCount() + "x " + inside.getHoverName().getString());
            }
            lines.add(Component.literal("Vision: " + count + "/27 stacks, " + items + " items")
                .withStyle(ChatFormatting.AQUA));
            for (String line : first) lines.add(Component.literal("  " + line).withStyle(ChatFormatting.GRAY));
            if (count > first.size()) lines.add(Component.literal("  ... and " + (count - first.size()) + " more stacks")
                .withStyle(ChatFormatting.DARK_GRAY));
        });
    }
}
