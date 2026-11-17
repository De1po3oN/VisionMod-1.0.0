package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

public final class VisionSpearSettingsScreen extends VisionPanelScreen {
    public VisionSpearSettingsScreen(){super("ЭФФЕКТЫ КОПЬЯ");}
    @Override protected void draw(GuiGraphics g,int mx,int my,float delta){
        int top=layout.bodyTop(),width=w-32;
        VisionFonts.fit(g,font,"Рывок — след; попадание — импульс",x+16,top,width,VisionMenuTheme.MUTED);
        button(g,x+16,top+24,width,24,"Длительность: "+VisionMotionEffects.spearDuration()+" мс",false,mx,my);
        button(g,x+16,top+58,width,24,"Масштаб: "+Math.round(VisionMotionEffects.spearSize()*100)+"%",false,mx,my);
        button(g,x+16,top+92,width,24,"Цвет эффекта  "+VisionWorldEffects.trailColorHex(VisionMotionEffects.spearColor()),true,mx,my);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(e.button()==0){
            int top=layout.bodyTop(),width=w-32;
            if(hit(e.x(),e.y(),x+16,top+24,width,24)){VisionMotionEffects.cycleSpearDuration();return true;}
            if(hit(e.x(),e.y(),x+16,top+58,width,24)){VisionMotionEffects.cycleSpearSize();return true;}
            if(hit(e.x(),e.y(),x+16,top+92,width,24)){
                minecraft.setScreen(new OreColorPickerScreen("Эффекты копья",VisionMotionEffects::spearColor,VisionMotionEffects::setSpearColor,this));return true;
            }
        }
        return super.mouseClicked(e,doubled);
    }
}
