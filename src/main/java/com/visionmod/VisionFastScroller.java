package com.visionmod;

import com.visionmod.mixin.ContainerScreenAccess;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

/** Inventory transfers through vanilla QUICK_MOVE; no hotbar jumps or inventory rewrites. */
public final class VisionFastScroller {
    private VisionFastScroller(){}
    public static void init(){
        ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
            if(!(screen instanceof AbstractContainerScreen<?> container)||screen instanceof CreativeModeInventoryScreen)return;
            FastScrollGesture gesture=new FastScrollGesture();
            ScreenMouseEvents.allowMouseClick(screen).register((scr,click)->{
                gesture.reset();
                if(click.button()==0&&ready(container)){
                    Slot slot=slotAt(container,click.x(),click.y());
                    gesture.beginDrag(slot==null?-1:slot.index);
                }
                return true;
            });
            ScreenMouseEvents.allowMouseDrag(screen).register((scr,click,dx,dy)->{
                if(click.button()!=0||!ready(container)){gesture.reset();return true;}
                Slot slot=slotAt(container,click.x(),click.y());
                if(!movable(slot)||!gesture.visit(slot.index))return true;
                transfer(container,slot);return false;
            });
            ScreenMouseEvents.allowMouseScroll(screen).register((scr,x,y,horizontal,vertical)->{
                if(!ready(container)||vertical==0){gesture.clearWheel();return true;}
                Slot slot=slotAt(container,x,y);
                if(!movable(slot)){gesture.clearWheel();return true;}
                if(gesture.scroll(slot.index,vertical))transfer(container,slot);
                return false;
            });
            ScreenMouseEvents.allowMouseRelease(screen).register((scr,click)->{if(click.button()==0)gesture.reset();return true;});
            ScreenEvents.remove(screen).register(scr->gesture.reset());
        });
    }
    private static boolean ready(AbstractContainerScreen<?> screen){
        Minecraft mc=Minecraft.getInstance();
        return VisionModClient.enabled&&VisionExtras.fastScroll&&mc.player!=null&&mc.gameMode!=null&&
            mc.screen==screen&&mc.hasShiftDown()&&mc.player.containerMenu==screen.getMenu()&&screen.getMenu().getCarried().isEmpty();
    }
    private static Slot slotAt(AbstractContainerScreen<?> screen,double x,double y){
        return ((ContainerScreenAccess)screen).visionmod$slotAt(x,y);
    }
    private static boolean movable(Slot slot){
        Minecraft mc=Minecraft.getInstance();
        return slot!=null&&slot.isActive()&&slot.hasItem()&&slot.mayPickup(mc.player);
    }
    private static void transfer(AbstractContainerScreen<?> screen,Slot slot){
        Minecraft mc=Minecraft.getInstance();
        mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId,slot.index,0,ClickType.QUICK_MOVE,mc.player);
    }
}
