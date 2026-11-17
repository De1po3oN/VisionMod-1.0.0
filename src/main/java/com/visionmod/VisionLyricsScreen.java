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
        line(g,"Текст в мире · плавно следует за игроком",top+89,VisionMenuTheme.MUTED);
    }
    private void line(GuiGraphics g,String text,int top,int color){VisionHudTheme.text(g,font,text,x+16,top,w-32,color);}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(e.button()==0&&hit(e.x(),e.y(),x+16,layout.bodyTop(),w-32,24)){VisionExtendedFeatures.toggle(3);return true;}
        return super.mouseClicked(e,doubled);
    }
}
