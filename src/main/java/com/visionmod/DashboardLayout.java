package com.visionmod;

/** Shared dashboard geometry for drawing and hit testing, in Minecraft GUI pixels. */
public record DashboardLayout(int width,int height,int sidebar,int contentWidth,int columns,int rows) {
    public static DashboardLayout of(int viewportWidth,int viewportHeight){
        int w=Math.min(820,viewportWidth-24),h=Math.min(440,viewportHeight-16);
        int sidebar=w>=590?138:w>=450?112:48,content=w-sidebar-32;
        boolean dense=h<330;
        int grid=dense?64:100,footer=h-(dense?30:36),gap=dense?8:10;
        int rows=Math.clamp((footer-grid-12+gap)/((dense?52:78)+gap),1,3);
        return new DashboardLayout(w,h,sidebar,content,content>=318?2:1,rows);
    }
    public int cards(){return columns*rows;}
    public int cardWidth(){return (contentWidth-10*(columns-1))/columns;}
    public boolean dense(){return height<330;}
    public boolean iconRail(){return sidebar<80;}
    public int contentLeft(){return sidebar+16;}
    public int cardHeight(){return Math.min(dense()?64:98,(navigationTop()-gridTop()-12-(rows-1)*gap())/rows);}
    public int gap(){return dense()?8:10;}
    public int gridTop(){return dense()?64:100;}
    public int cardLeft(int i){return contentLeft()+(i%columns)*(cardWidth()+10);}
    public int cardTop(int i){return gridTop()+(i/columns)*(cardHeight()+gap());}
    public int navigationTop(){return height-(dense()?30:36);}
    public int footerHeight(){return dense()?20:24;}
    private int tabStep(){return Math.min(dense()?16:34,(masterTop()-12-(dense()?64:86))/7);}
    public int tabTop(int i){return (dense()?64:86)+i*tabStep();}
    public int tabHeight(){return tabStep()-(dense()?2:6);}
    public int masterTop(){return height-(dense()?28:46);}
    public int masterHeight(){return dense()?18:30;}
    public int searchTop(){return dense()?12:16;}
    public int searchHeight(){return dense()?24:30;}
    public int titleTop(){return dense()?46:61;}
    public boolean compactFooter(){return dense()||contentWidth<380;}
    public int themeWidth(){return compactFooter()?26:68;}
    public int settingsWidth(){return compactFooter()?26:88;}
    public int themeLeft(){return width-16-themeWidth();}
    public int settingsLeft(){return themeLeft()-8-settingsWidth();}
}
