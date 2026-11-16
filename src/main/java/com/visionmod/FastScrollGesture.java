package com.visionmod;

import java.util.BitSet;

/** One visit per drag; fractional wheel input stays attached to the hovered slot. */
public final class FastScrollGesture {
    private final BitSet visited=new BitSet();
    private boolean dragging;
    private int wheelSlot=-1;
    private double wheel;
    public void beginDrag(int firstSlot){visited.clear();dragging=true;if(firstSlot>=0)visited.set(firstSlot);}
    public boolean visit(int slot){
        if(!dragging)beginDrag(-1);
        if(slot<0||visited.get(slot))return false;
        visited.set(slot);return true;
    }
    public boolean scroll(int slot,double amount){
        if(!Double.isFinite(amount)||slot<0){clearWheel();return false;}
        if(slot!=wheelSlot||Math.signum(amount)!=Math.signum(wheel)){wheel=0;wheelSlot=slot;}
        wheel+=Math.clamp(amount,-8,8);
        if(Math.abs(wheel)<1-1e-6)return false;
        wheel=0;return true;
    }
    public void clearWheel(){wheelSlot=-1;wheel=0;}
    public void reset(){dragging=false;visited.clear();clearWheel();}
}
