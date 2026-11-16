package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class ConfigsScreen extends VisionPanelScreen {
    private EditBox name;
    private List<String> profiles=List.of();
    private int selected;
    private String status="Профили хранятся локально";
    public ConfigsScreen(){super("ПРОФИЛИ");}
    private String current(){return profiles.isEmpty()?"":profiles.get(Math.min(selected,profiles.size()-1));}
    private void refresh(){String before=current();profiles=VisionProfiles.list();selected=Math.max(0,profiles.indexOf(before));}
    @Override protected void build(){
        String draft=name==null?"":name.getValue();
        name=new EditBox(font,x+16,layout.bodyTop(),w-32,20,Component.literal("Имя профиля"));
        name.setMaxLength(40);name.setValue(draft);name.setHint(Component.literal("Имя нового профиля"));addRenderableWidget(name);refresh();
    }
    @Override protected void draw(GuiGraphics g,int mx,int my,float delta){
        int top=layout.bodyTop(),bw=(w-42)/2;
        button(g,x+16,top+27,w-32,24,current().isBlank()?"Пока нет профилей":"Профиль: "+current(),false,mx,my);
        String[] actions={"Сохранить","Загрузить","Удалить","Обновить"};
        for(int i=0;i<4;i++)button(g,x+16+(i%2)*(bw+10),top+58+(i/2)*28,bw,22,actions[i],i==0,mx,my);
        VisionHudTheme.text(g,font,status,x+16,layout.bodyBottom()-10,w-32,VisionMenuTheme.MUTED);
        button(g,x+16,y+h-29,Math.min(176,w-119),20,"Открыть папку",false,mx,my);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(e.button()==0){
            int top=layout.bodyTop(),bw=(w-42)/2;
            if(hit(e.x(),e.y(),x+16,top+27,w-32,24)){if(!profiles.isEmpty())selected=(selected+1)%profiles.size();return true;}
            for(int i=0;i<4;i++)if(hit(e.x(),e.y(),x+16+(i%2)*(bw+10),top+58+(i/2)*28,bw,22)){
                try{
                    switch(i){case 0->{VisionProfiles.save(name.getValue().trim());status="Профиль сохранён";refresh();}
                        case 1->{VisionProfiles.load(current());status="Профиль загружен";}
                        case 2->{VisionProfiles.delete(current());status="Профиль удалён";refresh();}
                        case 3->{refresh();status="Список обновлён";}default->{}}
                    name.setHint(Component.literal(status));
                }catch(Exception ex){status=ex.getMessage();name.setHint(Component.literal(status));}
                return true;
            }
            if(hit(e.x(),e.y(),x+16,y+h-29,Math.min(176,w-119),20)){status=VisionProfiles.openFolder()?"Папка открыта":"config/visionmod/profiles";return true;}
        }
        return super.mouseClicked(e,doubled);
    }
}
