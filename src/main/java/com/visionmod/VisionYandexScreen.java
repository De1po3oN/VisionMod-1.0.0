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
        token=new MaskedTokenBox(font,x+16,layout.bodyTop()+35,w-32,20);
        token.setMaxLength(2048);token.setValue(draft);
        token.setHint(Component.literal("OAuth-токен · не пароль"));
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
    private void line(GuiGraphics g,String value,int yy,int color){VisionFonts.fit(g,font,value,x+16,yy,w-32,color);}
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
    /** Draw only bullets; neither the text renderer nor narrator receives the credential. */
    private static final class MaskedTokenBox extends EditBox {
        private final net.minecraft.client.gui.Font textFont;
        MaskedTokenBox(net.minecraft.client.gui.Font font,int x,int y,int width,int height){
            super(font,x,y,width,height,Component.literal("OAuth-токен Яндекс Музыки"));textFont=font;
        }
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float delta){
            g.fill(getX()-1,getY()-1,getX()+getWidth()+1,getY()+getHeight()+1,isFocused()?VisionMenuTheme.CYAN:0xFF586077);
            g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),0xFF111725);
            String value=getValue();int max=Math.max(1,(getWidth()-12)/Math.max(1,textFont.width("•")));
            String shown=value.isEmpty()?"OAuth-токен · не пароль":"•".repeat(Math.min(max,value.length()));
            g.drawString(textFont,shown,getX()+5,getY()+6,value.isEmpty()?VisionMenuTheme.MUTED:VisionMenuTheme.WHITE);
            if(isFocused()&&System.currentTimeMillis()%1000<500){int cx=getX()+5+textFont.width(value.isEmpty()?"":"•".repeat(Math.min(max,value.length())));g.fill(cx,getY()+4,cx+1,getY()+15,VisionMenuTheme.CYAN);}
        }
        @Override public net.minecraft.network.chat.MutableComponent createNarrationMessage(){return Component.literal("OAuth-токен Яндекс Музыки · содержимое скрыто");}
    }
}
