package com.visionmod;

/** Shared dashboard geometry for drawing and hit testing, in Minecraft GUI pixels. */
public record DashboardLayout(int width,int height,int sidebar,int contentWidth,int columns,int rows) {
    public static DashboardLayout of(int viewportWidth,int viewportHeight){
        int w=Math.min(720,viewportWidth-16),h=Math.min(430,viewportHeight-12);
        int sidebar=w>=470?128:86,content=w-sidebar-38;
        return new DashboardLayout(w,h,sidebar,content,content>=330?2:1,Math.max(1,Math.min(4,(h-148)/68)));
    }
    public int cards(){return columns*rows;}
    public int cardWidth(){return (contentWidth-10*(columns-1))/columns;}
    public int cardLeft(int i){return sidebar+22+(i%columns)*(cardWidth()+10);}
    public int cardTop(int i){return 89+(i/columns)*68;}
    public int navigationTop(){return height-48;}
}
