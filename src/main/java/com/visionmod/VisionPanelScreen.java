package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Shared adaptive editor frame; subclasses place their content inside body bounds. */
public abstract class VisionPanelScreen extends Screen {
    protected PanelLayout layout;
    protected int x,y,w,h;
    private final String caption;
    protected VisionPanelScreen(String caption){super(Component.literal(caption));this.caption=caption;}
    @Override protected final void init(){
        layout=PanelLayout.of(width,height);x=layout.x();y=layout.y();w=layout.width();h=layout.height();build();
        VisionMenuSession.entered(this);
    }
    protected void build(){}
    protected abstract void draw(GuiGraphics g,int mx,int my,float delta);
    @Override public final void renderBackground(GuiGraphics g,int mx,int my,float delta){}
    @Override public final void render(GuiGraphics g,int mx,int my,float delta){
        VisionGuiTheme.backdrop(g,width,height);VisionGuiTheme.frame(g,x,y,w,h);
        VisionGuiTheme.round(g,x+14,y+12,24,24,6,VisionGuiTheme.tint(VisionGuiTheme.CARD,.14f));
        VisionGuiTheme.icon(g,VisionGuiTheme.LOGO,x+20,y+18,12,VisionGuiTheme.accent());
        VisionFonts.heading(g,font,caption,x+48,y+18,w-64,VisionGuiTheme.WHITE);
        g.fill(x+16,y+43,x+w-16,y+44,VisionGuiTheme.BORDER);
        draw(g,mx,my,delta);
        button(g,x+w-93,y+h-29,77,20,"Готово",false,mx,my);
        super.render(g,mx,my,delta);
    }
    protected void button(GuiGraphics g,int bx,int by,int bw,int bh,String label,boolean active,int mx,int my){
        boolean hover=hit(mx,my,bx,by,bw,bh);
        VisionGuiTheme.button(g,bx,by,bw,bh,active,hover);
        VisionFonts.fit(g,font,label,bx+8,by+(bh-8)/2,bw-16,active?VisionGuiTheme.WHITE:VisionGuiTheme.MUTED);
    }
    protected static boolean hit(double mx,double my,int x,int y,int w,int h){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(e.button()==0&&hit(e.x(),e.y(),x+w-93,y+h-29,77,20)){onClose();return true;}
        return super.mouseClicked(e,doubled);
    }
    @Override public boolean keyPressed(KeyEvent event){return VisionMenuSession.closeFromKey(this,event)||super.keyPressed(event);}
    @Override public void onClose(){minecraft.setScreen(VisionMenuSession.dashboard());}
    @Override public boolean isPauseScreen(){return false;}
}
