package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Native media status: no extension, pairing, or config-file hunt. */
public final class VisionLyricsScreen extends VisionPanelScreen {
    public VisionLyricsScreen(){super("WORLD LYRICS");}
    @Override protected void build(){
        EditBox manual=new EditBox(font,x+16,layout.bodyBottom()-22,w-32,20,Component.literal("Свои строки"));
        manual.setMaxLength(120);manual.setValue(VisionExtendedFeatures.customText());manual.setHint(Component.literal("Свои строки через | — необязательно"));
        manual.setResponder(VisionExtendedFeatures::setCustomText);addRenderableWidget(manual);
    }
    @Override protected void draw(GuiGraphics g,int mx,int my,float delta){
        int top=layout.bodyTop();
        button(g,x+16,top,w-32,24,VisionExtendedFeatures.enabled(3)?"Включено · музыка определяется системой":"Включить World Lyrics",VisionExtendedFeatures.enabled(3),mx,my);
        line(g,VisionMediaSession.state(),top+35,VisionMenuTheme.WHITE);
        line(g,VisionLyricsSource.mediaConnected()?VisionLyricsSource.trackLabel():"Включи музыку и возвращайся в игру.",top+53,VisionMenuTheme.MUTED);
        line(g,VisionLyricsLookup.status(),top+71,VisionMenuTheme.CYAN);
        int row=layout.bodyBottom()-47,half=(w-42)/2;
        if(row>top+117)line(g,VisionLyricsSource.plainMode()?"Без таймкодов: выбирай строки стрелками ниже.":"Текст в мире · плавно следует за игроком",top+91,VisionMenuTheme.MUTED);
        button(g,x+16,row,half,22,"Яндекс Музыка",YandexMusicAccess.connected(),mx,my);
        if(VisionLyricsSource.plainMode()){
            int right=x+26+half;
            button(g,right,row,30,22,"<",false,mx,my);button(g,right+half-30,row,30,22,">",false,mx,my);
            g.drawCenteredString(font,(VisionLyricsSource.plainLineIndex()+1)+"/"+VisionLyricsSource.plainLineCount(),right+half/2,row+7,VisionMenuTheme.WHITE);
        }
    }
    private void line(GuiGraphics g,String text,int top,int color){VisionHudTheme.text(g,font,text,x+16,top,w-32,color);}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(e.button()==0&&hit(e.x(),e.y(),x+16,layout.bodyTop(),w-32,24)){VisionExtendedFeatures.toggle(3);return true;}
        int row=layout.bodyBottom()-47,half=(w-42)/2,right=x+26+half;
        if(e.button()==0&&hit(e.x(),e.y(),x+16,row,half,22)){minecraft.setScreen(new VisionYandexScreen());return true;}
        if(e.button()==0&&VisionLyricsSource.plainMode()){
            if(hit(e.x(),e.y(),right,row,30,22)){VisionLyricsSource.movePlainLine(-1);return true;}
            if(hit(e.x(),e.y(),right+half-30,row,30,22)){VisionLyricsSource.movePlainLine(1);return true;}
        }
        return super.mouseClicked(e,doubled);
    }
}
