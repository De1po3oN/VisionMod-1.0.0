package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Paged settings, with one source of coordinates for drawing and input. */
public abstract class VisionOptionsScreen extends VisionPanelScreen {
    private record Entry(String title,Supplier<String> value,Runnable action,int binding){}
    private final List<Entry> entries=new ArrayList<>();
    private int page,capture=-1;
    protected VisionOptionsScreen(String title){super(title);}
    protected void option(String title,Supplier<String> value,Runnable action){option(title,value,action,-1);}
    protected void option(String title,Supplier<String> value,Runnable action,int binding){entries.add(new Entry(title,value,action,binding));}
    protected abstract void options();
    @Override protected final void build(){entries.clear();options();page=Math.min(page,maxPage());}
    private int maxPage(){return Math.max(0,(entries.size()-1)/layout.pageSize());}
    @Override protected void draw(GuiGraphics g,int mx,int my,float delta){
        for(int i=0;i<layout.pageSize()&&page*layout.pageSize()+i<entries.size();i++){
            Entry e=entries.get(page*layout.pageSize()+i);int bx=layout.rowX(i),by=layout.rowY(i),bw=layout.rowWidth();
            VisionHudTheme.round(g,bx,by,bw,37,5,hit(mx,my,bx,by,bw,37)?0xFF30364B:0xFF232A3D);
            VisionHudTheme.text(g,font,e.title,bx+10,by+7,bw-20,VisionMenuTheme.WHITE);
            VisionHudTheme.text(g,font,e.value.get(),bx+10,by+22,bw-20,VisionMenuTheme.CYAN);
        }
        if(maxPage()>0){
            button(g,x+16,y+h-29,30,20,"<",page>0,mx,my);
            button(g,x+51,y+h-29,30,20,">",page<maxPage(),mx,my);
            g.drawString(font,(page+1)+" / "+(maxPage()+1),x+90,y+h-23,VisionMenuTheme.MUTED);
        }
        if(capture>=0){
            VisionHudTheme.round(g,x+12,y+50,w-24,h-88,6,0xF51A2032);
            g.drawCenteredString(font,"Нажми клавишу · Esc: убрать привязку",x+w/2,y+h/2,VisionMenuTheme.WHITE);
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(capture>=0){VisionHotkeys.assign(capture,-e.button()-1);capture=-1;return true;}
        if(e.button()==0){
            if(page>0&&hit(e.x(),e.y(),x+16,y+h-29,30,20)){page--;return true;}
            if(page<maxPage()&&hit(e.x(),e.y(),x+51,y+h-29,30,20)){page++;return true;}
        }
        for(int i=0;i<layout.pageSize()&&page*layout.pageSize()+i<entries.size();i++)if(hit(e.x(),e.y(),layout.rowX(i),layout.rowY(i),layout.rowWidth(),37)){
            Entry entry=entries.get(page*layout.pageSize()+i);
            if(e.button()==0)entry.action.run();else if(e.button()==2&&entry.binding>=0)capture=entry.binding;
            return true;
        }
        return super.mouseClicked(e,doubled);
    }
    @Override public boolean keyPressed(KeyEvent e){
        if(capture>=0){VisionHotkeys.assign(capture,e.key()==256?0:e.key());capture=-1;return true;}
        return super.keyPressed(e);
    }
}
