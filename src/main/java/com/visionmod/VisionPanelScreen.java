package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Shared adaptive editor frame; subclasses place their content inside body bounds. */
public abstract class VisionPanelScreen extends Screen {
    protected PanelLayout layout;
    protected int x,y,w,h;
    private final String caption;
    protected VisionPanelScreen(String caption){super(Component.literal(caption));this.caption=caption;}
    @Override protected final void init(){
        layout=PanelLayout.of(width,height);x=layout.x();y=layout.y();w=layout.width();h=layout.height();build();
    }
    protected void build(){}
    protected abstract void draw(GuiGraphics g,int mx,int my,float delta);
    @Override public final void renderBackground(GuiGraphics g,int mx,int my,float delta){}
    @Override public final void render(GuiGraphics g,int mx,int my,float delta){
        g.fill(0,0,width,height,0x80070A12);
        VisionHudTheme.round(g,x-1,y-1,w+2,h+2,8,0xFF46415F);
        VisionHudTheme.round(g,x,y,w,h,8,0xFF171C2B);
        g.fillGradient(x+12,y+1,x+w-12,y+3,VisionMenuTheme.CYAN,VisionMenuTheme.PURPLE);
        g.drawString(font,"VISION / "+caption,x+16,y+16,VisionMenuTheme.WHITE);
        g.fill(x+16,y+40,x+w-16,y+41,0xFF34394E);
        draw(g,mx,my,delta);
        button(g,x+w-93,y+h-29,77,20,"Готово",false,mx,my);
        super.render(g,mx,my,delta);
    }
    protected void button(GuiGraphics g,int bx,int by,int bw,int bh,String label,boolean active,int mx,int my){
        boolean hover=hit(mx,my,bx,by,bw,bh);
        VisionHudTheme.round(g,bx,by,bw,bh,4,active?0xFF494064:hover?0xFF34384F:0xFF252B3E);
        if(active)g.fill(bx+5,by+bh-1,bx+bw-5,by+bh,VisionMenuTheme.CYAN);
        VisionHudTheme.text(g,font,label,bx+8,by+(bh-8)/2,bw-16,active?VisionMenuTheme.WHITE:VisionMenuTheme.MUTED);
    }
    protected static boolean hit(double mx,double my,int x,int y,int w,int h){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(e.button()==0&&hit(e.x(),e.y(),x+w-93,y+h-29,77,20)){onClose();return true;}
        return super.mouseClicked(e,doubled);
    }
    @Override public void onClose(){minecraft.setScreen(new VisionDashboardScreen());}
    @Override public boolean isPauseScreen(){return false;}
}
