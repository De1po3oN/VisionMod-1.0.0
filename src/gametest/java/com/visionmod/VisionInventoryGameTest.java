package com.visionmod;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import static com.visionmod.GuiTestInput.*;

/** Real container, real input, real QUICK_MOVE packets and authoritative server counts. */
final class VisionInventoryGameTest {
    private static final BlockPos CHEST=new BlockPos(5,-60,0);
    static void run(ClientGameTestContext context,TestSingleplayerContext world)throws java.io.IOException{
        world.getServer().runCommand("clear @p");
        world.getServer().runCommand("setblock 5 -60 0 minecraft:chest");
        world.getServer().runOnServer(server->{
            var chest=(Container)server.overworld().getBlockEntity(CHEST);
            chest.setItem(0,new ItemStack(Items.STONE,32));chest.setItem(1,new ItemStack(Items.DIRT,16));chest.setItem(2,new ItemStack(Items.APPLE,8));
            server.getPlayerList().getPlayers().getFirst().openMenu((MenuProvider)chest);
        });
        context.waitFor(mc->mc.screen instanceof AbstractContainerScreen<?>,100);
        context.runOnClient(mc->{VisionExtras.fastScroll=true;VisionModClient.enabled=true;});
        context.getInput().holdShift();slot(context,0);context.getInput().scroll(-1);
        context.waitTicks(2);
        context.waitFor(mc->mc.player.getInventory().countItem(Items.STONE)==32,100);
        serverCounts(world,32,0,0);
        // First stack uses vanilla shift-click; dragging onto the second invokes Fast Scroller.
        slot(context,1);context.getInput().holdMouse(0);context.waitTicks(2);
        logInventory(context,"initial shift-click");
        slot(context,2);context.waitTicks(2);slot(context,1);slot(context,2);
        logInventory(context,"after drag");
        context.getInput().releaseMouse(0);
        context.waitFor(mc->mc.player.getInventory().countItem(Items.DIRT)==16&&mc.player.getInventory().countItem(Items.APPLE)==8,100);
        serverCounts(world,32,16,8);
        context.runOnClient(mc->check(mc.player.containerMenu.getCarried().isEmpty(),"Fast scrolling picked up a cursor stack"));
        VisualWorldGameTest.capture(context,"10-fast-scroller-transfers");
        int stone=find(context,Items.STONE),apple=find(context,Items.APPLE),dirt=find(context,Items.DIRT);
        context.getInput().releaseShift();slot(context,stone);context.getInput().scroll(1);context.waitTicks(2);serverCounts(world,32,16,8);
        context.getInput().holdShift();slot(context,stone);context.getInput().scroll(1);
        context.waitTicks(2);
        context.waitFor(mc->mc.player.getInventory().countItem(Items.STONE)==0,100);serverCounts(world,0,16,8);
        context.runOnClient(mc->VisionExtras.fastScroll=false);slot(context,apple);context.getInput().scroll(1);context.waitTicks(2);serverCounts(world,0,16,8);
        context.runOnClient(mc->{VisionExtras.fastScroll=true;VisionModClient.enabled=false;});slot(context,dirt);context.getInput().scroll(1);context.waitTicks(2);serverCounts(world,0,16,8);
        context.runOnClient(mc->VisionModClient.enabled=true);
        // A carried stack must keep vanilla behavior and cannot activate quick transfer.
        context.getInput().releaseShift();slot(context,apple);context.getInput().pressMouse(0);
        context.runOnClient(mc->check(mc.player.containerMenu.getCarried().getCount()==8,"Fixture did not pick up apples"));
        context.getInput().holdShift();slot(context,dirt);context.getInput().scroll(1);context.waitTicks(2);
        context.runOnClient(mc->check(mc.player.getInventory().countItem(Items.DIRT)==16&&mc.player.containerMenu.getCarried().getCount()==8,"Scroller changed inventory with a carried stack"));
        context.getInput().releaseShift();slot(context,apple);context.getInput().pressMouse(0);
        context.waitTicks(2);serverCounts(world,0,16,8);
        // Destination full: vanilla rejects the move without loss or duplication.
        world.getServer().runOnServer(server->{
            var chest=(Container)server.overworld().getBlockEntity(CHEST);
            for(int i=0;i<chest.getContainerSize();i++)chest.setItem(i,new ItemStack(Items.COBBLESTONE,64));
            chest.setChanged();
        });
        context.waitTicks(4);context.getInput().holdShift();slot(context,apple);context.getInput().scroll(1);context.waitTicks(3);serverCounts(world,0,16,8);
        context.getInput().releaseShift();context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);context.waitFor(mc->mc.screen==null,40);
        System.out.println("[VisionMod][INVENTORY] PASS: wheel and drag transfers, both directions, server item counts, no modifier, module/global OFF, carried stack and full container");
    }
    private static int find(ClientGameTestContext context,net.minecraft.world.item.Item item){
        return context.computeOnClient(mc->mc.player.containerMenu.slots.stream().filter(slot->slot.container==mc.player.getInventory()&&slot.getItem().is(item))
            .findFirst().orElseThrow().index);
    }
    private static void serverCounts(TestSingleplayerContext world,int stone,int dirt,int apple){
        world.getServer().runOnServer(server->{
            var inventory=server.getPlayerList().getPlayers().getFirst().getInventory();
            check(inventory.countItem(Items.STONE)==stone&&inventory.countItem(Items.DIRT)==dirt&&inventory.countItem(Items.APPLE)==apple,"Server rejected or duplicated an inventory transfer");
        });
    }
    private static void logInventory(ClientGameTestContext context,String stage){
        context.runOnClient(mc->System.out.println("[VisionMod][INVENTORY] "+stage+": dirt="+mc.player.getInventory().countItem(Items.DIRT)+", apple="+mc.player.getInventory().countItem(Items.APPLE)+", carried="+mc.player.containerMenu.getCarried().getCount()+", left="+mc.mouseHandler.isLeftPressed()+", shift="+mc.hasShiftDown()));
    }
}
