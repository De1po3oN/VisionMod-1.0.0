package com.visionmod;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Opt-in CI editor capture. Does not create a world or claim world-render QA. */
public final class VisionVisualCapture {
    private VisionVisualCapture(){}
    public static void init(){
        if(!"1".equals(System.getenv("VISIONMOD_VISUAL_QA")))return;
        ClientTickEvents.END_CLIENT_TICK.register(new java.util.function.Consumer<net.minecraft.client.Minecraft>(){
            private int ticks,stage=-1;
            @Override public void accept(net.minecraft.client.Minecraft mc){
                if(stage<0){if(mc.screen==null||mc.getOverlay()!=null)return;
                    if(ticks==0)System.out.println("[VisionMod][VISUAL] Initial screen: "+mc.screen.getClass().getSimpleName());
                    if(++ticks<40)return;stage=0;ticks=0;}
                if(ticks++!=0){if(ticks>=80){ticks=0;stage++;}return;}
                switch(stage){
                    case 0->mc.setScreen(new VisionDashboardScreen());
                    case 1->mc.setScreen(new VisionAppearanceScreen());
                    case 2->mc.setScreen(new VisionTrailSettingsScreen());
                    case 3->mc.setScreen(new VisionCrosshairEditorScreen());
                    case 4->mc.setScreen(new OreColorPickerScreen(0));
                    case 5->mc.setScreen(new ConfigsScreen());
                    case 6->mc.setScreen(new PerformanceScreen());
                    case 7->mc.setScreen(new VisionLyricsScreen());
                    default->{return;}
                }
                System.out.println("[VisionMod][VISUAL] "+stage);
            }
        }::accept);
    }
}
