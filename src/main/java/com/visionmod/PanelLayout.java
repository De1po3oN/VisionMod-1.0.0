package com.visionmod;

/** Editor coordinates independent of rendering; minimum supported Minecraft GUI 320×240. */
public record PanelLayout(int x,int y,int width,int height) {
    public static PanelLayout of(int width,int height){
        int w=Math.min(540,width-16),h=Math.min(354,height-12);
        return new PanelLayout((width-w)/2,(height-h)/2,w,h);
    }
    public int bodyTop(){return y+55;}
    public int bodyBottom(){return y+height-39;}
    public int columns(){return width>=400?2:1;}
    public int rows(){return Math.max(1,(height-94)/43);}
    public int pageSize(){return columns()*rows();}
    public int rowWidth(){return (width-32-(columns()-1)*10)/columns();}
    public int rowX(int i){return x+16+(i%columns())*(rowWidth()+10);}
    public int rowY(int i){return bodyTop()+(i/columns())*43;}
}
