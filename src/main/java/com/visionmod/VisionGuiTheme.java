package com.visionmod;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Mod menu surfaces and original vector icons; separate from the in-game HUD. */
public final class VisionGuiTheme {
    public static final int BACK=0xFF11151D, RAIL=0xFF161C26, CARD=0xFF1B222E,
        HOVER=0xFF242E3D, BORDER=0xFF2B3545, WHITE=0xFFF0F3FA,
        MUTED=0xFF95A2B7, FAINT=0xFF65748C;
    public static final int SEARCH=7, SETTINGS=8, LEFT=9, PALETTE=10, POWER=11, LOGO=12, KEY=13, CLOSE=14;
    private VisionGuiTheme() { }
    public static int accent(){return VisionMenuTheme.CYAN;}
    public static int tint(int background,float amount){return VisionGeometry.blendRgb(background,accent(),amount);}

    /** One pixel coverage at corner edges avoids the hard stair-step of filled discs. */
    public static void round(GuiGraphics g,int x,int y,int w,int h,int radius,int color){
        if(w<=0||h<=0)return;
        int r=Math.clamp(radius,0,Math.min(w/2,h/2));
        if(r==0){g.fill(x,y,x+w,y+h,color);return;}
        g.fill(x,y+r,x+w,y+h-r,color);
        for(int row=0;row<r;row++){
            double inset=r-Math.sqrt(r*r-(r-row-.5)*(r-row-.5));
            int cut=(int)Math.ceil(inset);
            int edge=VisionGeometry.withAlpha(color,(int)((color>>>24)*(cut-inset)));
            cornerRow(g,x,y+row,w,cut,color,edge);cornerRow(g,x,y+h-row-1,w,cut,color,edge);
        }
    }
    private static void cornerRow(GuiGraphics g,int x,int y,int w,int cut,int color,int edge){
        g.fill(x+cut,y,x+w-cut,y+1,color);
        if(cut>0){g.fill(x+cut-1,y,x+cut,y+1,edge);g.fill(x+w-cut,y,x+w-cut+1,y+1,edge);}
    }
    public static void frame(GuiGraphics g,int x,int y,int w,int h){
        round(g,x-8,y+1,w+16,h+18,17,0x1702050B);
        round(g,x-4,y+2,w+8,h+8,13,0x3A02050B);
        round(g,x-1,y-1,w+2,h+2,11,BORDER);
        round(g,x,y,w,h,10,BACK);
    }
    public static void backdrop(GuiGraphics g,int width,int height){
        g.fillGradient(0,0,width,height,0x7A0B1019,0xB4050810);
    }
    public static void button(GuiGraphics g,int x,int y,int w,int h,boolean selected,boolean hover){
        round(g,x,y,w,h,5,selected?tint(CARD,.18f):hover?HOVER:CARD);
        if(hover||selected)round(g,x,y,2,h,1,selected?accent():FAINT);
    }
    public static void icon(GuiGraphics g,int kind,int x,int y,int size,int color){
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(size/12f,size/12f);
        switch(kind){
            case 0 -> {line(g,0,6,3,2,color);line(g,3,2,8,2,color);line(g,8,2,12,6,color);line(g,12,6,8,10,color);line(g,8,10,3,10,color);line(g,3,10,0,6,color);round(g,5,4,3,4,1,color);}
            case 1 -> {for(int a=0;a<2;a++)for(int b=0;b<2;b++)round(g,a*7,b*7,5,5,1,color);}
            case 2 -> {ring(g,6,6,4,color);line(g,6,0,6,4,color);line(g,6,9,6,12,color);line(g,0,6,4,6,color);line(g,9,6,12,6,color);}
            case 3 -> {line(g,6,0,12,3,color);line(g,12,3,12,9,color);line(g,12,9,6,12,color);line(g,6,12,0,9,color);line(g,0,9,0,3,color);line(g,0,3,6,0,color);line(g,0,3,6,6,color);line(g,6,6,12,3,color);line(g,6,6,6,12,color);}
            case 4 -> {line(g,0,7,3,7,color);line(g,3,7,5,1,color);line(g,5,1,8,11,color);line(g,8,11,10,5,color);line(g,10,5,12,5,color);}
            case 5 -> {for(int a=0;a<3;a++){int xx=1+a*5;line(g,xx,0,xx,12,color);round(g,xx-1,2+a*3,3,3,1,color);}}
            case 6 -> {round(g,0,8,2,4,1,color);round(g,5,4,2,8,1,color);round(g,10,0,2,12,1,color);}
            case SEARCH -> {ring(g,5,5,4,color);line(g,8,8,12,12,color);}
            case SETTINGS -> {ring(g,6,6,3,color);for(int a=0;a<8;a++){double t=a*Math.PI/4;line(g,6+(float)Math.cos(t)*4,6+(float)Math.sin(t)*4,6+(float)Math.cos(t)*6,6+(float)Math.sin(t)*6,color);}}
            case LEFT -> {line(g,8,2,4,6,color);line(g,4,6,8,10,color);}
            case PALETTE -> {ring(g,6,6,5,color);round(g,3,3,2,2,1,color);round(g,7,3,2,2,1,color);round(g,3,7,2,2,1,color);}
            case POWER -> {line(g,6,0,6,6,color);line(g,2,3,1,7,color);line(g,1,7,3,11,color);line(g,3,11,9,11,color);line(g,9,11,11,7,color);line(g,11,7,10,3,color);}
            case LOGO -> {line(g,1,2,6,11,color);line(g,6,11,11,2,color);line(g,5,2,7,2,color);}
            case KEY -> {round(g,0,3,12,7,2,color);g.fill(2,5,10,8,CARD);g.fill(4,6,8,7,color);}
            case CLOSE -> {line(g,2,2,10,10,color);line(g,2,10,10,2,color);}
            default -> { }
        }
        g.pose().popMatrix();
    }
    public static void chevron(GuiGraphics g,boolean right,int x,int y,int color){
        if(right){line(g,x+3,y+2,x+7,y+6,color);line(g,x+7,y+6,x+3,y+10,color);}
        else icon(g,LEFT,x,y,12,color);
    }
    private static void ring(GuiGraphics g,float x,float y,float radius,int color){
        for(int i=0;i<12;i++){
            double a=i*Math.PI/6,b=(i+1)*Math.PI/6;
            line(g,x+(float)Math.cos(a)*radius,y+(float)Math.sin(a)*radius,x+(float)Math.cos(b)*radius,y+(float)Math.sin(b)*radius,color);
        }
    }
    private static void line(GuiGraphics g,float ax,float ay,float bx,float by,int color){
        float dx=bx-ax,dy=by-ay;
        g.pose().pushMatrix();g.pose().translate(ax,ay);g.pose().rotate((float)Math.atan2(dy,dx));
        g.fill(0,0,Math.max(1,Math.round((float)Math.hypot(dx,dy))),1,color);g.pose().popMatrix();
    }
    public static void tooltip(GuiGraphics g,Font font,String label,int mx,int my,int width,int height){
        int w=Math.min(width-12,VisionFonts.width(font,label)+18),x=Math.clamp(mx+10,6,width-w-6),y=Math.min(my+15,height-24);
        round(g,x-1,y-1,w+2,21,5,BORDER);round(g,x,y,w,19,4,RAIL);
        VisionFonts.fit(g,font,label,x+9,y+5,w-18,WHITE);
    }
}
