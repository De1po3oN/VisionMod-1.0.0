package com.visionmod;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

/** Navigation lives only for this Minecraft process, including editor pages and search. */
public final class VisionMenuSession {
    private static Screen last;
    private static VisionDashboardScreen dashboard;
    private static KeyMapping binding;
    private static boolean suppressUntilRelease;
    private VisionMenuSession(){}
    public static void bind(KeyMapping key){binding=key;}
    public static void entered(Screen screen){
        last=screen;if(screen instanceof VisionDashboardScreen root)dashboard=root;
    }
    public static VisionDashboardScreen dashboard(){
        if(dashboard==null)dashboard=new VisionDashboardScreen();
        return dashboard;
    }
    public static void tick(Minecraft mc){
        if(binding==null)return;
        boolean requested=false;while(binding.consumeClick())requested=true;
        if(suppressUntilRelease){if(!binding.isDown())suppressUntilRelease=false;return;}
        if(requested&&mc.screen==null)mc.setScreen(last==null?dashboard():last);
    }
    public static boolean closeFromKey(Screen screen,KeyEvent event){
        if(binding==null||!binding.matches(event))return false;
        last=screen;suppressUntilRelease=true;Minecraft.getInstance().setScreen(null);return true;
    }
}
