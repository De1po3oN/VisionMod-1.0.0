package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.awt.Color;
import java.util.Locale;

/** Responsive HSV studio; the actual saved color is always reflected in the field. */
public final class OreColorPickerScreen extends VisionPanelScreen {
    private final int ore;
    private float hue,sat,value;
    private int size,sx,sy,hx,fieldX,fieldWidth;
    private EditBox field;
    private boolean squareDrag,hueDrag,changing;
    private String error="";
    public OreColorPickerScreen(int ore){super("ЦВЕТ РУДЫ");this.ore=Math.clamp(ore,0,OreColors.NAMES.length-1);load(OreColors.getColor(this.ore));}
    private void load(int color){float[] hsv=Color.RGBtoHSB((color>>16)&255,(color>>8)&255,color&255,null);hue=hsv[0];sat=hsv[1];value=hsv[2];}
    @Override protected void build(){
        size=Math.min(165,layout.bodyBottom()-layout.bodyTop()-18);sx=x+16;sy=layout.bodyTop();hx=sx+size+10;
        fieldX=hx+24;fieldWidth=x+w-16-fieldX;
        field=new EditBox(font,fieldX,sy+53,fieldWidth,20,Component.literal("HEX / цвет"));
        field.setMaxLength(24);field.setValue(String.format(Locale.ROOT,"#%06X",OreColors.getColor(ore)&0xFFFFFF));
        field.setResponder(text->{
            if(changing)return;
            String raw=text.strip().toLowerCase(Locale.ROOT);
            raw=switch(raw){case "red","красный"->"ff5555";case "blue","синий"->"5555ff";case "green","зелёный"->"55ff88";
                case "pink","розовый"->"ff88c9";case "white","белый"->"ffffff";case "black","чёрный"->"000000";default->raw.replace("#","").replace("0x","");};
            if(!raw.matches("[0-9a-f]{6}")){error="HEX: #RRGGBB";return;}
            int color=0xFF000000|Integer.parseInt(raw,16);load(color);OreColors.setColor(ore,color);error="";
        });addRenderableWidget(field);
    }
    private void update(){
        int color=Color.HSBtoRGB(hue,sat,value)|0xFF000000;OreColors.setColor(ore,color);
        changing=true;field.setValue(String.format(Locale.ROOT,"#%06X",color&0xFFFFFF));changing=false;error="";
    }
    @Override protected void draw(GuiGraphics g,int mx,int my,float delta){
        for(int col=0;col<size;col++)g.fill(sx+col,sy,sx+col+1,sy+size,Color.HSBtoRGB(hue,col/(float)(size-1),1)|0xFF000000);
        g.fillGradient(sx,sy,sx+size,sy+size,0,0xFF000000);
        for(int row=0;row<size;row++)g.fill(hx,sy+row,hx+12,sy+row+1,Color.HSBtoRGB(row/(float)(size-1),1,1)|0xFF000000);
        int xx=sx+Math.round(sat*(size-1)),yy=sy+Math.round((1-value)*(size-1));
        VisionHudTheme.ring(g,xx,yy,4,1,0xFFFFFFFF);
        int hy=sy+Math.round(hue*(size-1));g.fill(hx-2,hy-1,hx+14,hy+2,0xFFFFFFFF);
        VisionHudTheme.round(g,fieldX,sy,fieldWidth,36,4,OreColors.getColor(ore));
        VisionHudTheme.text(g,font,OreColors.NAMES[ore],fieldX,sy+41,fieldWidth,VisionMenuTheme.WHITE);
        VisionHudTheme.text(g,font,error.isBlank()?"Сохраняется сразу":error,fieldX,sy+82,fieldWidth,error.isBlank()?VisionMenuTheme.MUTED:VisionMenuTheme.RED);
    }
    private void pick(double mx,double my){
        if(squareDrag){sat=EffectMath.unit((float)(mx-sx)/(size-1));value=1-EffectMath.unit((float)(my-sy)/(size-1));}
        if(hueDrag)hue=EffectMath.unit((float)(my-sy)/(size-1));update();
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(e.button()==0){squareDrag=hit(e.x(),e.y(),sx,sy,size,size);hueDrag=hit(e.x(),e.y(),hx,sy,12,size);if(squareDrag||hueDrag){pick(e.x(),e.y());return true;}}
        return super.mouseClicked(e,doubled);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(squareDrag||hueDrag){pick(e.x(),e.y());return true;}return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(squareDrag||hueDrag){squareDrag=hueDrag=false;return true;}return super.mouseReleased(e);}
}
