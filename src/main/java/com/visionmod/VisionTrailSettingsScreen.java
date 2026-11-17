package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** A live ribbon preview with color input and settings that fit every supported GUI size. */
public final class VisionTrailSettingsScreen extends VisionPanelScreen {
    private static final int[][] PALETTES={{0xFF9CE4FF,0xFFC8A5FF},{0xFFFF99CC,0xFFFFD397},{0xFF99EDCE,0xFF91B8FF},{0xFFF4F0FF,0xFFAAA4D9},{0xFFFFAA91,0xFFE9A2FF}};
    private EditBox start,end;
    private int palette;
    private String error="";
    public VisionTrailSettingsScreen(){super("ТРЕЙЛЫ");}
    @Override protected void build(){
        int half=(w-42)/2,top=layout.bodyTop();
        start=new EditBox(font,x+16,top+12,half,20,Component.literal("Начало"));
        end=new EditBox(font,x+26+half,top+12,half,20,Component.literal("Конец"));
        start.setMaxLength(20);end.setMaxLength(20);
        start.setValue(VisionWorldEffects.trailColorHex(VisionWorldEffects.trailStartColor()));
        end.setValue(VisionWorldEffects.trailColorHex(VisionWorldEffects.trailEndColor()));
        start.setResponder(v->apply(0,v));end.setResponder(v->apply(1,v));addRenderableWidget(start);addRenderableWidget(end);
    }
    private void apply(int index,String value){try{VisionWorldEffects.setTrailColor(index,VisionWorldEffects.parseTrailColor(value));error="";}catch(IllegalArgumentException e){error="HEX #RRGGBB или R,G,B";}}
    @Override protected void draw(GuiGraphics g,int mx,int my,float delta){
        int top=layout.bodyTop(),half=(w-42)/2;
        VisionFonts.draw(g,font,"НАЧАЛО",x+16,top,VisionMenuTheme.MUTED);VisionFonts.draw(g,font,"КОНЕЦ",x+26+half,top,VisionMenuTheme.MUTED);
        int yy=top+43,pw=w-32;
        VisionHudTheme.round(g,x+16,yy,pw,18,4,0xFF101625);
        for(int i=0;i<48;i++){
            int l=x+16+i*pw/48,r=x+16+(i+1)*pw/48;
            int color=VisionWorldEffects.trailSampleColor(i/47f,System.nanoTime()/50_000_000L);
            for(int strip=0;strip<3;strip++)g.fill(l,yy+4+strip*4,r,yy+8+strip*4,VisionGeometry.withAlpha(color,strip==1?210:65));
        }
        int third=(w-48)/3;
        String[] modes={"SOLID","GRADIENT","RAINBOW"};
        for(int i=0;i<3;i++)button(g,x+16+i*(third+8),top+69,third,22,modes[i],VisionWorldEffects.trailMode()==i,mx,my);
        button(g,x+16,top+99,half,22,"Ширина: "+String.format(java.util.Locale.ROOT,"%.2f",VisionWorldEffects.trailWidth()),false,mx,my);
        button(g,x+26+half,top+99,half,22,"Плотность: "+VisionWorldEffects.trailOpacity(),false,mx,my);
        button(g,x+16,y+h-29,Math.min(176,w-119),20,"Цветовая пара",false,mx,my);
        if(h>280)VisionFonts.fit(g,font,error.isEmpty()?"Цвет и режим сохраняются автоматически":error,x+16,top+141,w-32,error.isEmpty()?VisionMenuTheme.MUTED:VisionMenuTheme.RED);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(e.button()==0){
            int top=layout.bodyTop(),half=(w-42)/2,third=(w-48)/3;
            for(int i=0;i<3;i++)if(hit(e.x(),e.y(),x+16+i*(third+8),top+69,third,22)){VisionWorldEffects.setTrailMode(i);return true;}
            if(hit(e.x(),e.y(),x+16,top+99,half,22)){VisionWorldEffects.cycleTrailWidth();return true;}
            if(hit(e.x(),e.y(),x+26+half,top+99,half,22)){VisionWorldEffects.cycleTrailOpacity();return true;}
            if(hit(e.x(),e.y(),x+16,y+h-29,Math.min(176,w-119),20)){
                int[] colors=PALETTES[(palette++)%PALETTES.length];start.setValue(VisionWorldEffects.trailColorHex(colors[0]));end.setValue(VisionWorldEffects.trailColorHex(colors[1]));return true;
            }
        }
        return super.mouseClicked(e,doubled);
    }
}
