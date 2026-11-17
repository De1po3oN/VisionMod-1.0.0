package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Optional authenticated lyrics access. The player itself uses the OS media session. */
public final class VisionYandexScreen extends VisionPanelScreen {
    private String draft="";
    private EditBox token;
    public VisionYandexScreen(){super("ЯНДЕКС МУЗЫКА");}
    @Override protected void build(){
        token=new EditBox(font,x+16,layout.bodyTop()+35,w-32,20,Component.literal("OAuth-токен Яндекс Музыки"));
        token.setMaxLength(2048);token.setValue(draft);
        token.setHint(Component.literal("OAuth-токен · не пароль"));
        token.setFormatter((value,cursor)->Component.literal("•".repeat(value.length())).getVisualOrderText());
        token.setResponder(value->draft=value);addRenderableWidget(token);
    }
    @Override protected void draw(GuiGraphics g,int mx,int my,float delta){
        int top=layout.bodyTop();
        line(g,"Плеер и браузер определяются системой автоматически.",top,VisionMenuTheme.WHITE);
        line(g,"Токен открывает доступ к доступным аккаунту текстам.",top+17,VisionMenuTheme.MUTED);
        button(g,x+16,top+59,(w-42)/2,22,"Подключить",YandexMusicAccess.connected(),mx,my);
        button(g,x+26+(w-42)/2,top+59,(w-42)/2,22,"Отключить",false,mx,my);
        line(g,YandexMusicAccess.status(),top+90,VisionMenuTheme.CYAN);
        line(g,"Токен хранится только до закрытия Minecraft.",top+109,VisionMenuTheme.MUTED);
    }
    private void line(GuiGraphics g,String value,int yy,int color){VisionHudTheme.text(g,font,value,x+16,yy,w-32,color);}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubled){
        int top=layout.bodyTop();
        if(event.button()==0&&hit(event.x(),event.y(),x+16,top+59,(w-42)/2,22)){
            if(YandexMusicAccess.connect(draft)){draft="";token.setValue("");}return true;
        }
        if(event.button()==0&&hit(event.x(),event.y(),x+26+(w-42)/2,top+59,(w-42)/2,22)){
            YandexMusicAccess.disconnect();draft="";token.setValue("");return true;
        }
        return super.mouseClicked(event,doubled);
    }
    @Override public void onClose(){minecraft.setScreen(new VisionLyricsScreen());}
}
