package com.visionmod;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Consistent HUD surfaces. Fixed rectangle counts, no framebuffer effects. */
public final class VisionHudTheme {
    private VisionHudTheme(){}
    public static void round(GuiGraphics g,int x,int y,int w,int h,int radius,int color){
        if(w<=0||h<=0)return;
        int r=Math.min(radius,Math.min(w/2,h/2));
        g.fill(x+r,y,x+w-r,y+h,color);g.fill(x,y+r,x+r,y+h-r,color);g.fill(x+w-r,y+r,x+w,y+h-r,color);
        for(int i=0;i<r;i++){
            int inset=(int)Math.ceil(r-Math.sqrt(r*r-(r-i-.5)*(r-i-.5)));
            g.fill(x+inset,y+i,x+r,y+i+1,color);g.fill(x+w-r,y+i,x+w-inset,y+i+1,color);
            g.fill(x+inset,y+h-i-1,x+r,y+h-i,color);g.fill(x+w-r,y+h-i-1,x+w-inset,y+h-i,color);
        }
    }
    public static void panel(GuiGraphics g,int x,int y,int w,int h,int accent){
        round(g,x-1,y,w+2,h+2,6,0x38040710);
        round(g,x,y,w,h,5,VisionGeometry.withAlpha(0xFF121828,VisionAppearance.opacity));
        g.fill(x+7,y,x+w-7,y+1,VisionGeometry.withAlpha(accent,100));
        g.fill(x+1,y+7,x+2,y+h-7,VisionGeometry.withAlpha(accent,130));
    }
    public static void text(GuiGraphics g,Font font,String text,int x,int y,int width,int color){
        g.drawString(font,font.plainSubstrByWidth(text,Math.max(0,width)),x,y,color);
    }
    public static void meter(GuiGraphics g,int x,int y,int w,int h,float value,int color){
        round(g,x,y,w,h,2,0xFF30364A);
        int amount=Math.round(w*EffectMath.unit(value));
        if(amount>0)round(g,x,y,amount,h,2,color);
        if(amount>3)g.fill(x+2,y,x+amount-1,y+1,VisionGeometry.withAlpha(0xFFFFFFFF,60));
    }
    public static void ring(GuiGraphics g,int x,int y,int radius,float progress,int color){
        int steps=32,active=Math.round(steps*EffectMath.unit(progress));
        for(int i=0;i<steps;i++){
            double angle=-Math.PI/2+i*Math.PI*2/steps;
            int xx=x+(int)Math.round(Math.cos(angle)*radius),yy=y+(int)Math.round(Math.sin(angle)*radius);
            g.fill(xx,yy,xx+2,yy+2,i<active?color:0xFF41465C);
        }
    }
}
