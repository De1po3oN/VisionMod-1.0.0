package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.awt.Color;
import java.util.Locale;

/** Responsive HSV studio; the actual saved color is always reflected in the field. */
public final class OreColorPickerScreen extends VisionPanelScreen {
    private final java.util.function.IntSupplier getColor;
    private final java.util.function.IntConsumer setColor;
    private final String label;
    private final net.minecraft.client.gui.screens.Screen parent;
    private static final java.util.Map<String,String> NAMED=java.util.Map.ofEntries(
        java.util.Map.entry("red","ff5555"),java.util.Map.entry("красный","ff5555"),
        java.util.Map.entry("green","55ff55"),java.util.Map.entry("зелёный","55ff55"),java.util.Map.entry("зеленый","55ff55"),
        java.util.Map.entry("blue","5555ff"),java.util.Map.entry("синий","5555ff"),
        java.util.Map.entry("yellow","ffff55"),java.util.Map.entry("жёлтый","ffff55"),java.util.Map.entry("желтый","ffff55"),
        java.util.Map.entry("cyan","55ffff"),java.util.Map.entry("голубой","55ffff"),
        java.util.Map.entry("pink","ff55aa"),java.util.Map.entry("розовый","ff55aa"),
        java.util.Map.entry("purple","aa55ff"),java.util.Map.entry("фиолетовый","aa55ff"),
        java.util.Map.entry("orange","ffaa33"),java.util.Map.entry("gold","ffd700"),java.util.Map.entry("lime","00ff00"),
        java.util.Map.entry("white","ffffff"),java.util.Map.entry("белый","ffffff"),
        java.util.Map.entry("black","000000"),java.util.Map.entry("чёрный","000000"),java.util.Map.entry("черный","000000"));
    private float hue,sat,value;
    private int size,sx,sy,hx,fieldX,fieldWidth;
    private EditBox field;
    private boolean squareDrag,hueDrag,changing;
    private String error="";
    public OreColorPickerScreen(int ore){this(OreColors.NAMES[Math.clamp(ore,0,9)],()->OreColors.getColor(Math.clamp(ore,0,9)),color->OreColors.setColor(Math.clamp(ore,0,9),color),null);}
    public OreColorPickerScreen(String label,java.util.function.IntSupplier getColor,java.util.function.IntConsumer setColor,net.minecraft.client.gui.screens.Screen parent){
        super("ЦВЕТОВАЯ СТУДИЯ");this.label=label;this.getColor=getColor;this.setColor=setColor;this.parent=parent;load(getColor.getAsInt());
    }
    private void load(int color){float[] hsv=Color.RGBtoHSB((color>>16)&255,(color>>8)&255,color&255,null);hue=hsv[0];sat=hsv[1];value=hsv[2];}
    @Override protected void build(){
        size=Math.min(165,layout.bodyBottom()-layout.bodyTop()-18);sx=x+16;sy=layout.bodyTop();hx=sx+size+10;
        fieldX=hx+24;fieldWidth=x+w-16-fieldX;
        field=new EditBox(font,fieldX,sy+53,fieldWidth,20,Component.literal("HEX / цвет"));
        field.setMaxLength(24);field.setValue(String.format(Locale.ROOT,"#%06X",getColor.getAsInt()&0xFFFFFF));
        field.setResponder(text->{
            if(changing)return;
            String raw=text.strip().toLowerCase(Locale.ROOT);
            raw=NAMED.getOrDefault(raw,raw.replace("#","").replace("0x",""));
            if(!raw.matches("[0-9a-f]{6}")){error="HEX: #RRGGBB";return;}
            int color=0xFF000000|Integer.parseInt(raw,16);load(color);setColor.accept(color);error="";
        });addRenderableWidget(field);
    }
    private void update(){
        int color=Color.HSBtoRGB(hue,sat,value)|0xFF000000;setColor.accept(color);
        changing=true;field.setValue(String.format(Locale.ROOT,"#%06X",color&0xFFFFFF));changing=false;error="";
    }
    @Override protected void draw(GuiGraphics g,int mx,int my,float delta){
        for(int col=0;col<size;col++)g.fill(sx+col,sy,sx+col+1,sy+size,Color.HSBtoRGB(hue,col/(float)(size-1),1)|0xFF000000);
        g.fillGradient(sx,sy,sx+size,sy+size,0,0xFF000000);
        for(int row=0;row<size;row++)g.fill(hx,sy+row,hx+12,sy+row+1,Color.HSBtoRGB(row/(float)(size-1),1,1)|0xFF000000);
        int xx=sx+Math.round(sat*(size-1)),yy=sy+Math.round((1-value)*(size-1));
        VisionHudTheme.ring(g,xx,yy,4,1,0xFFFFFFFF);
        int hy=sy+Math.round(hue*(size-1));g.fill(hx-2,hy-1,hx+14,hy+2,0xFFFFFFFF);
        VisionHudTheme.round(g,fieldX,sy,fieldWidth,36,4,getColor.getAsInt());
        VisionHudTheme.text(g,font,label,fieldX,sy+41,fieldWidth,VisionMenuTheme.WHITE);
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
    @Override public void onClose(){if(parent==null)super.onClose();else minecraft.setScreen(parent);}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(squareDrag||hueDrag){squareDrag=hueDrag=false;return true;}return super.mouseReleased(e);}
}
