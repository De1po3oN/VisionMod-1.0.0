package com.visionmod;

import com.visionmod.mixin.ContainerScreenAccess;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

final class GuiTestInput {
    static void move(ClientGameTestContext context,double x,double y){
        double[] physical=context.computeOnClient(mc->new double[]{
            x*mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth(),
            y*mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight()});
        context.getInput().setCursorPos(physical[0],physical[1]);
    }
    static void click(ClientGameTestContext context,double x,double y,int button){move(context,x,y);context.getInput().pressMouse(button);}
    static void slot(ClientGameTestContext context,int index){
        double[] point=context.computeOnClient(mc->{
            var screen=(AbstractContainerScreen<?>)mc.screen;
            var access=(ContainerScreenAccess)screen;var slot=screen.getMenu().getSlot(index);
            return new double[]{access.visionmod$left()+slot.x+8,access.visionmod$top()+slot.y+8};
        });
        move(context,point[0],point[1]);
    }
    static Object field(Object instance,String name){
        for(Class<?> type=instance.getClass();type!=null;type=type.getSuperclass())try{
            var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(instance);
        }catch(NoSuchFieldException ignored){}catch(ReflectiveOperationException error){throw new AssertionError(error);}
        throw new AssertionError("Missing UI state "+name);
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
