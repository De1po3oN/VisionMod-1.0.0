package com.visionmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class VisionHotbarAnimation {
    private static final HotbarTween TWEEN=new HotbarTween();
    private static ClientLevel world;
    private static long frames;
    private VisionHotbarAnimation(){}
    public static boolean enabled(){return VisionModClient.enabled&&VisionAppearance.animations&&VisionAppearance.smoothHotbar;}
    public static float offset(int slot){
        ClientLevel current=Minecraft.getInstance().level;
        if(current!=world){world=current;TWEEN.reset();}
        float position=TWEEN.sample(slot,System.nanoTime(),enabled());frames++;
        return position-slot*20f;
    }
    public static float itemScale(int slot){
        return enabled()?1+.10f*Math.max(0,1-Math.abs(TWEEN.value()-slot*20f)/20f):1;
    }
    static float position(){return TWEEN.value();}
    static long frames(){return frames;}
}
