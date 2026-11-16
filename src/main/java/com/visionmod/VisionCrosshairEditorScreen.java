package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

/** Pixel painting with drag, right-button erase and an actual reticle preview. */
public final class VisionCrosshairEditorScreen extends VisionPanelScreen {
    private int canvas,cx,cy,cell,lastX=-1,lastY=-1;
    private boolean painting,erase;
    public VisionCrosshairEditorScreen(){super("ПРИЦЕЛ");}
    @Override protected void build(){
        int max=Math.min(192,layout.bodyBottom()-layout.bodyTop()-14);
        cell=Math.max(1,max/VisionExtras.crosshairGrid);canvas=cell*VisionExtras.crosshairGrid;
        cx=x+16;cy=layout.bodyTop();
    }
    @Override protected void draw(GuiGraphics g,int mx,int my,float delta){
        VisionHudTheme.round(g,cx-3,cy-3,canvas+6,canvas+6,4,0xFF363D55);g.fill(cx,cy,cx+canvas,cy+canvas,0xFF101626);
        int grid=VisionExtras.crosshairGrid;
        for(int row=0;row<grid;row++)for(int col=0;col<grid;col++)if(VisionExtras.pixel(col,row))g.fill(cx+col*cell,cy+row*cell,cx+(col+1)*cell,cy+(row+1)*cell,VisionExtras.crosshairColor);
        for(int i=0;i<=grid;i+=Math.max(1,grid/8)){int d=i*cell;g.fill(cx+d,cy,cx+d+1,cy+canvas,0x244E5A79);g.fill(cx,cy+d,cx+canvas,cy+d+1,0x244E5A79);}
        int bx=cx+canvas+14,bw=x+w-16-bx;
        for(int i=0;i<3;i++)button(g,bx,cy+i*25,bw,21,(16<<i)+" × "+(16<<i),grid==(16<<i),mx,my);
        button(g,bx,cy+80,bw,21,"Размер: "+VisionExtras.crosshairSize,false,mx,my);
        button(g,bx,cy+106,bw,21,"Цвет",false,mx,my);
        if(cy+155<layout.bodyBottom())button(g,bx,cy+132,bw,21,"Сбросить",false,mx,my);
        else button(g,x+16,y+h-29,80,20,"Сбросить",false,mx,my);
    }
    private void paint(MouseButtonEvent e){
        int col=(int)(e.x()-cx)/cell,row=(int)(e.y()-cy)/cell;
        if(!hit(e.x(),e.y(),cx,cy,canvas,canvas)||(col==lastX&&row==lastY))return;
        int fromX=lastX<0?col:lastX,fromY=lastY<0?row:lastY;
        int steps=Math.max(Math.abs(col-fromX),Math.abs(row-fromY));
        for(int i=0;i<=steps;i++){
            float t=steps==0?0:i/(float)steps;
            VisionExtras.setPixel(Math.round(fromX+(col-fromX)*t),Math.round(fromY+(row-fromY)*t),!erase);
        }
        lastX=col;lastY=row;
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if((e.button()==0||e.button()==1)&&hit(e.x(),e.y(),cx,cy,canvas,canvas)){
            painting=true;erase=e.button()==1;lastX=lastY=-1;paint(e);return true;
        }
        if(e.button()==0){
            int bx=cx+canvas+14,bw=x+w-16-bx;
            for(int i=0;i<3;i++)if(hit(e.x(),e.y(),bx,cy+i*25,bw,21)){VisionExtras.setCrosshairGrid(16<<i);build();return true;}
            if(hit(e.x(),e.y(),bx,cy+80,bw,21)){VisionExtras.setCrosshairSize(VisionExtras.crosshairSize%4+1);return true;}
            if(hit(e.x(),e.y(),bx,cy+106,bw,21)){minecraft.setScreen(new OreColorPickerScreen("Прицел",()->VisionExtras.crosshairColor,color->{VisionExtras.crosshairColor=color;VisionExtras.save();},this));return true;}
            if((cy+155<layout.bodyBottom()&&hit(e.x(),e.y(),bx,cy+132,bw,21))||
                (cy+155>=layout.bodyBottom()&&hit(e.x(),e.y(),x+16,y+h-29,80,20))){VisionExtras.resetPixels();return true;}
        }
        return super.mouseClicked(e,doubled);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(painting){paint(e);return true;}return super.mouseDragged(e,dx,dy);}
    @Override public void onClose(){if(painting)VisionExtras.commitPixels();super.onClose();}
    @Override public void removed(){if(painting){painting=false;VisionExtras.commitPixels();}super.removed();}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(painting){painting=false;VisionExtras.commitPixels();return true;}return super.mouseReleased(e);}
}
