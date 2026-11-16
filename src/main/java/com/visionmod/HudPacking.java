package com.visionmod;

import java.util.Collection;

/** Bounded placement for untouched HUD panels; user-dragged coordinates take precedence. */
public final class HudPacking {
    public record Rect(int x,int y,int width,int height){
        public boolean overlaps(Rect other){
            return x<other.x+other.width+4&&x+width+4>other.x&&y<other.y+other.height+4&&y+height+4>other.y;
        }
    }
    private HudPacking(){}
    public static Rect find(int x,int y,int width,int height,int viewportWidth,int viewportHeight,Collection<Rect> occupied){
        int maxX=Math.max(0,viewportWidth-width),maxY=Math.max(0,viewportHeight-height);
        Rect preferred=new Rect(Math.clamp(x,0,maxX),Math.clamp(y,0,maxY),width,height);
        if(free(preferred,occupied))return preferred;
        int[] xs=new int[occupied.size()*2+3],ys=new int[xs.length];
        xs[0]=preferred.x;xs[1]=Math.min(8,maxX);xs[2]=Math.max(0,maxX-8);
        ys[0]=preferred.y;ys[1]=Math.min(8,maxY);ys[2]=Math.max(0,maxY-8);
        int i=3;
        for(Rect box:occupied){
            xs[i]=Math.clamp(box.x-width-6,0,maxX);ys[i++]=Math.clamp(box.y-height-6,0,maxY);
            xs[i]=Math.clamp(box.x+box.width+6,0,maxX);ys[i++]=Math.clamp(box.y+box.height+6,0,maxY);
        }
        Rect best=preferred;long distance=Long.MAX_VALUE;
        for(int xx:xs)for(int yy:ys){
            long dx=xx-preferred.x,dy=yy-preferred.y,cost=dx*dx+dy*dy;
            if(cost>=distance)continue;
            Rect candidate=new Rect(xx,yy,width,height);
            if(free(candidate,occupied)){best=candidate;distance=cost;}
        }
        // A very small or crowded viewport may have no solution; keep the panel reachable.
        return best;
    }
    private static boolean free(Rect candidate,Collection<Rect> occupied){
        for(Rect box:occupied)if(candidate.overlaps(box))return false;
        return true;
    }
}
