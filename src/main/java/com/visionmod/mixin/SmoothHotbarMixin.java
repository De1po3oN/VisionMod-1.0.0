package com.visionmod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.visionmod.VisionHotbarAnimation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** Wrap only vanilla hotbar drawing; selected slot and packets keep vanilla timing. */
@Mixin(Gui.class)
public abstract class SmoothHotbarMixin {
    @Shadow @Final private static Identifier HOTBAR_SELECTION_SPRITE;
    @WrapOperation(method="renderItemHotbar",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"),require=1)
    private void visionmod$selection(GuiGraphics graphics,RenderPipeline pipeline,Identifier sprite,int x,int y,int width,int height,Operation<Void> original){
        if(!sprite.equals(HOTBAR_SELECTION_SPRITE)||Minecraft.getInstance().player==null){original.call(graphics,pipeline,sprite,x,y,width,height);return;}
        float offset=VisionHotbarAnimation.offset(Minecraft.getInstance().player.getInventory().getSelectedSlot());
        graphics.pose().pushMatrix();
        try{graphics.pose().translate(offset,0);original.call(graphics,pipeline,sprite,x,y,width,height);}
        finally{graphics.pose().popMatrix();}
    }
    @WrapOperation(method="renderItemHotbar",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Gui;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V"),require=1)
    private void visionmod$item(Gui gui,GuiGraphics graphics,int x,int y,DeltaTracker tracker,Player player,ItemStack item,int seed,Operation<Void> original){
        float scale=seed>=1&&seed<=9?VisionHotbarAnimation.itemScale(seed-1):1;
        graphics.pose().pushMatrix();
        try{
            graphics.pose().translate(x+8,y+8);graphics.pose().scale(scale,scale);graphics.pose().translate(-x-8,-y-8);
            original.call(gui,graphics,x,y,tracker,player,item,seed);
        }finally{graphics.pose().popMatrix();}
    }
}
