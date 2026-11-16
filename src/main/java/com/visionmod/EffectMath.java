package com.visionmod;

/** Bounded curves shared by world effects and their regression tests. */
public final class EffectMath {
    private EffectMath(){}
    public static float unit(float value){return Float.isFinite(value)?Math.clamp(value,0,1):0;}
    public static float ease(float t){t=unit(t);return 1-(1-t)*(1-t)*(1-t);}
    public static float fade(float t){t=unit(t);return (1-t)*(1-t);}
    public static float smooth(float current,float target,float seconds){
        if(!Float.isFinite(current))current=0;
        if(!Float.isFinite(target))target=0;
        if(!Float.isFinite(seconds))seconds=0;
        return current+(target-current)*(1-(float)Math.exp(-12*Math.clamp(seconds,0,.1f)));
    }
    public static int blend(int a,int b,float t){
        t=unit(t);return 0xFF000000|((int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t)<<16)|
            ((int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t)<<8)|(int)((a&255)*(1-t)+(b&255)*t);
    }
    public static float ringRadius(float progress,float max){return .18f+ease(progress)*max;}
}
