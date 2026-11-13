package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Custom 16/32/64 editor with a fixed 128px canvas and independent reticle size. */
public final class VisionCrosshairEditorScreen extends Screen {
    private static final int CANVAS = 128;
    public VisionCrosshairEditorScreen() { super(Component.literal("Vision / Crosshair Studio")); }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta) { }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        super.render(g,mx,my,delta);
        VisionMenuTheme.panel(g,font,width,height,"CROSSHAIR STUDIO","DESIGN  /  SCALE  /  COLOR");
        int x=VisionMenuTheme.x(width), y=VisionMenuTheme.y(height);
        int grid=VisionExtras.crosshairGrid, cell=CANVAS/grid;
        int gx=x+142,gy=y+81;
        g.fill(gx-3,gy-3,gx+CANVAS+3,gy+CANVAS+3,VisionMenuTheme.CYAN);
        g.fill(gx,gy,gx+CANVAS,gy+CANVAS,0xFF101824);
        for(int row=0;row<grid;row++) for(int col=0;col<grid;col++) {
            if (!VisionExtras.pixel(col,row)) continue;
            int xx=gx+col*cell, yy=gy+row*cell;
            g.fill(xx,yy,xx+cell,yy+cell,VisionExtras.crosshairColor);
        }
        int line=grid==64?8:grid==32?4:2;
        for(int j=0;j<=grid;j+=line) {
            int coord=j*cell;
            g.fill(gx+coord,gy,gx+coord+1,gy+CANVAS,0x335B789C);
            g.fill(gx,gy+coord,gx+CANVAS,gy+coord+1,0x335B789C);
        }
        g.drawString(font, "GRID",x+278,y+75,VisionMenuTheme.MUTED);
        for(int i=0;i<3;i++){
            int n=16<<i;
            VisionMenuTheme.option(g,font,x+280,y+90+i*25,146,21,n+" x "+n,
                VisionExtras.crosshairGrid==n,inside(mx,my,x+280,y+90+i*25,146,21));
        }
        g.drawString(font,"SIZE",x+278,y+176,VisionMenuTheme.MUTED);
        for(int i=0;i<4;i++){
            VisionMenuTheme.option(g,font,x+280+i*38,y+189,34,23,""+(i+1),
                VisionExtras.crosshairSize==i+1,inside(mx,my,x+280+i*38,y+189,34,23));
        }
        VisionMenuTheme.option(g,font,x+142,y+224,135,23,"COLOR  >",false,
            inside(mx,my,x+142,y+224,135,23));
        VisionMenuTheme.button(g,font,x+282,y+224,144,23,"RESET",inside(mx,my,x+282,y+224,144,23));
        VisionMenuTheme.button(g,font,x+142,y+251,284,17,"BACK TO EXTRAS",inside(mx,my,x+142,y+251,284,17));
    }
    private static boolean inside(double mx,double my,int x,int y,int w,int h){return VisionMenuTheme.inside(mx,my,x,y,w,h);}
    @Override public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if(click.button()!=GLFW.GLFW_MOUSE_BUTTON_LEFT && click.button()!=GLFW.GLFW_MOUSE_BUTTON_RIGHT)
            return super.mouseClicked(click,doubled);
        int x=VisionMenuTheme.x(width), y=VisionMenuTheme.y(height);
        double mx=click.x(),my=click.y();
        int grid=VisionExtras.crosshairGrid, cell=CANVAS/grid;
        if(inside(mx,my,x+142,y+81,CANVAS,CANVAS)) {
            int col=(int)(mx-(x+142))/cell,row=(int)(my-(y+81))/cell;
            VisionExtras.togglePixel(col,row);return true;
        }
        for(int i=0;i<3;i++) if(inside(mx,my,x+280,y+90+i*25,146,21)){
            VisionExtras.setCrosshairGrid(16<<i);return true;
        }
        for(int i=0;i<4;i++)if(inside(mx,my,x+280+i*38,y+189,34,23)){
            VisionExtras.setCrosshairSize(i+1);return true;
        }
        if(inside(mx,my,x+142,y+224,135,23)){VisionExtras.cycleCrosshairColor();return true;}
        if(inside(mx,my,x+282,y+224,144,23)){VisionExtras.resetPixels();return true;}
        if(inside(mx,my,x+142,y+251,284,17)){minecraft.setScreen(new VisionScreen(VisionScreen.Page.EXTRAS));return true;}
        return super.mouseClicked(click,doubled);
    }
    @Override public void onClose(){minecraft.setScreen(new VisionScreen(VisionScreen.Page.EXTRAS));}
    @Override public boolean isPauseScreen(){return false;}
}
