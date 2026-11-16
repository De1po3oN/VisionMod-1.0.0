package com.visionmod;

/** Retargetable 140 ms selection motion, sampled at render rate, never mutates inventory. */
public final class HotbarTween {
    private float from,value,target;
    private long started;
    private boolean initialized;
    public float sample(int slot,long now,boolean animate){
        float next=Math.clamp(slot,0,8)*20f;
        if(!initialized||!animate){initialized=true;from=value=target=next;started=now;return value;}
        float t=Math.clamp((now-started)/140_000_000f,0,1);
        float eased=1-(1-t)*(1-t)*(1-t);
        value=from+(target-from)*eased;
        if(next!=target){from=value;target=next;started=now;}
        return value;
    }
    public float value(){return value;}
    public void reset(){initialized=false;}
}
