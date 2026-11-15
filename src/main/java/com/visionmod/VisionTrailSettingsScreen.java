package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Built-in trail editor, independent of existing dashboard pagination. */
public final class VisionTrailSettingsScreen extends Screen {
    private static final int[][] PALETTES={
        {0xFF67E8F9,0xFFB66DFF},{0xFFFF78B9,0xFFFFD48C},
        {0xFF7FE8AA,0xFF51BAFF},{0xFFFFFFFF,0xFFDEE1FF},
        {0xFFFF6262,0xFFFFB763},{0xFFAA66FF,0xFFFD83C9}
    };
    private EditBox start,end;
    private int x,y,w,h,palette;
    private String error="";
    public VisionTrailSettingsScreen() { super(Component.literal("Vision Trails - RGB and Gradients")); }
    @Override protected void init() {
        w=Math.min(430,width-24);h=Math.min(294,height-14);x=(width-w)/2;y=(height-h)/2;
        int half=(w-50)/2;
        start=new EditBox(font,x+18,y+65,half,20,Component.literal("Trail start"));
        end=new EditBox(font,x+30+half,y+65,half,20,Component.literal("Trail end"));
        start.setMaxLength(20);end.setMaxLength(20);
        start.setValue(VisionWorldEffects.trailColorHex(VisionWorldEffects.trailStartColor()));
        end.setValue(VisionWorldEffects.trailColorHex(VisionWorldEffects.trailEndColor()));
        start.setResponder(value->apply(0,value));end.setResponder(value->apply(1,value));
        addRenderableWidget(start);addRenderableWidget(end);
    }
    private void apply(int which,String value) {
        try { VisionWorldEffects.setTrailColor(which,VisionWorldEffects.parseTrailColor(value));error=""; }
        catch(IllegalArgumentException e) { error="Use #RRGGBB or R,G,B, values 0-255"; }
    }
    private void palette() {
        int[] p=PALETTES[(++palette)%PALETTES.length];
        VisionWorldEffects.setTrailColor(0,p[0]);VisionWorldEffects.setTrailColor(1,p[1]);
        start.setValue(VisionWorldEffects.trailColorHex(p[0]));
        end.setValue(VisionWorldEffects.trailColorHex(p[1]));
        error="";
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta){}
    private void button(GuiGraphics g,int bx,int by,int bw,int bh,String label,int mx,int my) {
        boolean hover=inside(mx,my,bx,by,bw,bh);
        g.fill(bx-1,by-1,bx+bw+1,by+bh+1,0xFF504868);
        g.fill(bx,by,bx+bw,by+bh,hover?0xFF433657:0xFF29253E);
        g.drawCenteredString(font,label,bx+bw/2,by+(bh-8)/2,0xFFF9F4FF);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xA9000611);
        g.fill(x-2,y-2,x+w+2,y+h+2,0xFF655587);
        g.fill(x,y,x+w,y+h,0xF4141728);
        g.fill(x+8,y+7,x+w-8,y+43,0xFF29283F);
        g.drawString(font,"VISION / TRAIL CUSTOMIZER",x+18,y+14,0xFFF8F4FF);
        g.drawString(font,"SOLID   GRADIENT   RAINBOW",x+18,y+29,0xFF94EAF8);
        int half=(w-50)/2;
        g.drawString(font,"START RGB / HEX",x+18,y+53,0xFFAAA5C4);
        g.drawString(font,"END RGB / HEX",x+30+half,y+53,0xFFAAA5C4);
        g.drawString(font,"PREVIEW",x+18,y+93,0xFFAAA5C4);
        int bx=x+18,by=y+106,pw=w-36;
        for(int i=0;i<40;i++){
            int l=bx+(int)(i*pw/40f),r=bx+(int)((i+1)*pw/40f);
            int color=VisionWorldEffects.trailSampleColor(i/39f,System.nanoTime()/50_000_000L);
            g.fill(l,by,Math.max(l+1,r),by+21,color);
        }
        int third=(w-48)/3;
        button(g,x+18,y+140,third,26,"SOLID",mx,my);
        button(g,x+24+third,y+140,third,26,"GRADIENT",mx,my);
        button(g,x+30+2*third,y+140,third,26,"RAINBOW",mx,my);
        g.drawCenteredString(font,"MODE: "+VisionWorldEffects.trailModeLabel(),x+w/2,y+176,0xFF8CE3CF);
        int wide=(w-42)/2;
        button(g,x+18,y+192,wide,25,"WIDTH: "+String.format(java.util.Locale.ROOT,"%.2f",VisionWorldEffects.trailWidth()),mx,my);
        button(g,x+24+wide,y+192,wide,25,"ALPHA: "+VisionWorldEffects.trailOpacity(),mx,my);
        button(g,x+18,y+230,wide,27,"NEXT PALETTE",mx,my);
        button(g,x+24+wide,y+230,wide,27,"BACK",mx,my);
        g.drawString(font,error.isBlank()?"Changes save automatically":error,
            x+18,y+267,error.isBlank()?0xFFAAA5C4:0xFFFF91AF);
        super.render(g,mx,my,delta);
    }
    private static boolean inside(double px,double py,int x,int y,int w,int h){
        return px>=x&&py>=y&&px<x+w&&py<y+h;
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled) {
        if(e.button()==GLFW.GLFW_MOUSE_BUTTON_LEFT){
            double mx=e.x(),my=e.y();
            int third=(w-48)/3,wide=(w-42)/2;
            if(inside(mx,my,x+18,y+140,third,26)){VisionWorldEffects.setTrailMode(0);return true;}
            if(inside(mx,my,x+24+third,y+140,third,26)){VisionWorldEffects.setTrailMode(1);return true;}
            if(inside(mx,my,x+30+2*third,y+140,third,26)){VisionWorldEffects.setTrailMode(2);return true;}
            if(inside(mx,my,x+18,y+192,wide,25)){VisionWorldEffects.cycleTrailWidth();return true;}
            if(inside(mx,my,x+24+wide,y+192,wide,25)){VisionWorldEffects.cycleTrailOpacity();return true;}
            if(inside(mx,my,x+18,y+230,wide,27)){palette();return true;}
            if(inside(mx,my,x+24+wide,y+230,wide,27)){onClose();return true;}
        }
        return super.mouseClicked(e,doubled);
    }
    @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(new VisionDashboardScreen());}
    @Override public boolean isPauseScreen(){return false;}
}
