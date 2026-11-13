package com.visionmod;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.nio.file.*;
import java.io.*;
import java.util.Properties;

/** Dedicated lightweight 2D panel aesthetic: no vanilla button textures or shader blur. */
public final class VisionMenuTheme {
    public static final int WIDTH = 450;
    public static final int HEIGHT = 316;
    public static final String VERSION = "1.0.0";
    private static final int[] PRIMARY = {0xFF46E4F5, 0xFFFF76B7, 0xFF67F0B1, 0xFFFFC567, 0xFF8C9DFF, 0xFFECECFD};
    private static final int[] SECONDARY = {0xFFB68AFF, 0xFFFFB56E, 0xFF3AD9FF, 0xFFFF7289, 0xFF60F5FF, 0xFF98B6DE};
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("visionmod/theme.properties");
    private static int palette = 0;
    public static int CYAN = PRIMARY[0];
    public static int PURPLE = SECONDARY[0];
    public static final int WHITE = 0xFFF1F5FE;
    public static final int MUTED = 0xFF91A5BE;
    public static final int GREEN = 0xFF63EDB0;
    public static final int RED = 0xFFF97995;
    public static final int BACK = 0xF40C111E;
    public static final int SURFACE = 0xFF1B2638;
    public static final int HOVER = 0xFF283B50;

    private VisionMenuTheme() { }
    public static int x(int width) { return (width-WIDTH)/2; }
    public static int y(int height) { return Math.max(3,(height-HEIGHT)/2); }
    public static boolean inside(double mx,double my,int x,int y,int w,int h) {
        return mx>=x&&mx<x+w&&my>=y&&my<y+h;
    }
    public static int palette() { return palette; }
    public static void setPalette(int index) {
        palette = Math.floorMod(index,PRIMARY.length);
        CYAN=PRIMARY[palette]; PURPLE=SECONDARY[palette]; save();
    }
    public static void load() {
        if (!Files.isRegularFile(FILE)) return;
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(FILE)) {
            p.load(in); palette=Math.floorMod(Integer.parseInt(p.getProperty("palette","0")),PRIMARY.length);
            CYAN=PRIMARY[palette]; PURPLE=SECONDARY[palette];
        } catch(Exception e) { System.err.println("[VisionMod] Theme load failed: "+e); }
    }
    private static void save() {
        try { Files.createDirectories(FILE.getParent());
            Properties p = new Properties(); p.setProperty("palette",Integer.toString(palette));
            try(OutputStream out=Files.newOutputStream(FILE)){p.store(out,"Vision Mod theme");}
        } catch(Exception e) { System.err.println("[VisionMod] Theme save failed: "+e); }
    }

    public static void panel(GuiGraphics g,Font font,int screenWidth,int screenHeight,String title,String subtitle) {
        int x=x(screenWidth),y=y(screenHeight),r=x+WIDTH,b=y+HEIGHT;
        // Soft multi-layer relief. No framebuffer effects, shaders or blur passes.
        g.fill(x-14,y+10,r+14,b+17,0x3C02070E);
        g.fill(x-8,y+5,r+8,b+9,0x64090E19);
        g.fill(x-2,y-2,r+2,b+2,0xFF30445C);
        g.fillGradient(x,y,r,b,0xF51A2436,0xF5070D18);
        g.fill(x,y,r,y+3,0xFF152435);
        g.fillGradient(x+17,y+2,r-17,y+4,CYAN,PURPLE);
        g.fillGradient(x+1,y+54,r-1,y+55,0xFF30465A,0xFF27354A);
        g.fillGradient(x+1,b-34,r-1,b-33,CYAN,PURPLE);
        g.fill(x+1,y+1,x+9,y+8,CYAN);
        g.fill(r-10,b-10,r-1,b-1,PURPLE);
        g.fill(x+14,y+13,x+18,y+41,CYAN);
        // Typographic hierarchy: larger bold heading and quiet tracking caption.
        g.pose().pushMatrix();
        g.pose().translate(x+29,y+13);
        g.pose().scale(1.19f,1.19f);
        g.drawString(font,Component.literal(title).withStyle(ChatFormatting.BOLD),0,0,WHITE);
        g.pose().popMatrix();
        String shortTitle=subtitle;
        while (!shortTitle.isEmpty() && font.width(shortTitle)>260) shortTitle=shortTitle.substring(0,shortTitle.length()-1);
        g.drawString(font,shortTitle,x+29,y+36,MUTED);
        g.drawString(font,"v"+VERSION,r-58,y+17,CYAN);
        g.drawString(font,"COLOR",r-157,y+27,0xFF718DA6);
        for(int i=0;i<PRIMARY.length;i++) {
            int sx=r-155+i*19, sy=y+39;
            g.fill(sx-1,sy-1,sx+15,sy+9,i==palette?WHITE:0xFF33475C);
            g.fill(sx,sy,sx+14,sy+8,PRIMARY[i]);
        }
        footer(g,font,x,y);
    }
    private static void footer(GuiGraphics g,Font font,int x,int y) {
        g.drawString(font,"TG: @cocok_ali",x+17,y+HEIGHT-23,CYAN);
        String server="For: mc.showtime.su";
        g.drawString(font,server,x+WIDTH-font.width(server)-17,y+HEIGHT-23,PURPLE);
    }
    public static int paletteHit(double mx,double my,int screenWidth,int screenHeight) {
        int x=x(screenWidth),y=y(screenHeight),r=x+WIDTH;
        for(int i=0;i<PRIMARY.length;i++) if(inside(mx,my,r-156+i*19,y+38,17,12)) return i;
        return -1;
    }
    public static void sidebar(GuiGraphics g,Font font,int x,int y,int active,double mx,double my) {
        String[] tabs={"VISUALS","ORES","EXTRAS","PERFORMANCE","CONFIGS","ADDONS"};
        g.fillGradient(x+11,y+62,x+119,y+268,0xFA101B2C,0xFA0B1424);
        g.fill(x+118,y+62,x+119,y+268,0xFF2A4257);
        for(int i=0;i<tabs.length;i++) {
            int bx=x+18,by=y+70+i*32;
            boolean on=i==active,hover=inside(mx,my,bx,by,93,27);
            if(on || hover) {
                g.fillGradient(bx,by,bx+93,by+25,on?0xFF184A5B:0xFF26364A,0xFF192739);
            }
            g.fill(bx,by+4,bx+(on?3:1),by+23,on?CYAN:0xFF425E72);
            g.drawString(font,on?Component.literal(tabs[i]).withStyle(ChatFormatting.BOLD):Component.literal(tabs[i]),
                bx+12,by+10,on?WHITE: (hover?WHITE:MUTED));
            if(on)g.fill(bx+10,by+25,bx+84,by+28,CYAN);
        }
        g.drawString(font,"OPEN BETA",x+23,y+257,0xFF52677E);
    }
    // Page navigation deliberately uses warm amber, unlike the user-selected
    // accent palette, so it stays recognizable in every theme.
    public static final int PAGE_GOLD = 0xFFF6BC61;
    public static final int PAGE_GOLD_SOFT = 0xFFD99A42;
    private static final int PAGE_DARK_TEXT = 0xFF18212D;
    private static final int PAGE_PREV_OFFSET = 130;
    private static final int PAGE_NEXT_OFFSET = 344;
    private static final int PAGE_TOP_OFFSET = 254;

    public static void pageNavigator(GuiGraphics g, Font font, int x, int y,
                                     int current, int count, double mouseX, double mouseY) {
        if (count < 2) return;
        final int top = y + PAGE_TOP_OFFSET;
        final int previousX = x + PAGE_PREV_OFFSET;
        final int nextX = x + PAGE_NEXT_OFFSET;
        // Distinct control group, not an ambiguous all-width "click for next" bar.
        g.fill(x+128, top-3, x+436, top+27, 0xB009111F);
        g.fill(x+129, top-2, x+435, top-1, 0xFF7B562E);
        pageButton(g, font, previousX, top, 94, 23, "< НАЗАД",
            current > 0, inside(mouseX,mouseY,previousX,top,94,23));
        pageButton(g, font, nextX, top, 90, 23, "ДАЛЬШЕ >",
            current < count-1, inside(mouseX,mouseY,nextX,top,90,23));
        g.fillGradient(x+228,top,x+340,top+23,0xFF3A2B20,0xFF201C23);
        g.fill(x+228,top,x+340,top+1,PAGE_GOLD_SOFT);
        g.drawCenteredString(font, "СТР. " + (current+1) + " / " + count,
            x+284, top+7, PAGE_GOLD);
    }

    private static void pageButton(GuiGraphics g, Font font, int x, int y, int w, int h,
                                   String label, boolean enabled, boolean hovered) {
        int upper = !enabled ? 0xFF334052 : hovered ? 0xFFFFD385 : PAGE_GOLD;
        int lower = !enabled ? 0xFF202B39 : hovered ? 0xFFEAAA4F : 0xFFBB7730;
        g.fill(x+1,y+2,x+w+2,y+h+2,0x90060A12);
        g.fillGradient(x,y,x+w,y+h,upper,lower);
        g.fill(x,y,x+w,y+2,!enabled ? 0xFF475468 : 0xFFFFE3B1);
        g.drawCenteredString(font,label,x+w/2,y+7,enabled ? PAGE_DARK_TEXT : 0xFF8796AB);
    }

    /** -1=previous, +1=next, 0=none/disabled. Uses exactly the rendered hitboxes. */
    public static int pageNavigationHit(double mx,double my,int x,int y,int current,int count) {
        int top=y+PAGE_TOP_OFFSET;
        if (current>0 && inside(mx,my,x+PAGE_PREV_OFFSET,top,94,23)) return -1;
        if (current<count-1 && inside(mx,my,x+PAGE_NEXT_OFFSET,top,90,23)) return 1;
        return 0;
    }

    public static void moduleRow(GuiGraphics g,Font font,int x,int y,int w,int h,String label,boolean enabled,String binding,boolean hovered) {
        g.fill(x+2,y+2,x+w+2,y+h+2,0x77060911);
        g.fillGradient(x,y,x+w,y+h,hovered?0xFF2B4356:0xFF202D3E,0xFF101A2A);
        g.fill(x,y,x+w,y+1,enabled?0xFF2A6E7B:0xFF314052);
        g.fill(x,y,x+3,y+h,enabled?CYAN:0xFF526A81);
        g.drawString(font,label,x+10,y+8,WHITE);
        if(binding!=null)g.drawString(font,binding,x+10,y+h-13,MUTED);
        int px=x+w-32,py=y+12;
        g.fill(px,py,px+26,py+13,enabled?0xFF1C6070:0xFF425266);
        g.fill(px+(enabled?14:3),py+2,px+(enabled?23:12),py+11,enabled?CYAN:0xFF92A5B8);
    }
    public static void actionRow(GuiGraphics g,Font font,int x,int y,int w,int h,String label,String binding,boolean hovered) {
        g.fillGradient(x,y,x+w,y+h,hovered?0xFF2A4358:SURFACE,0xFF101B2C);
        g.fill(x,y,x+3,y+h,PURPLE);
        g.drawString(font,label,x+10,y+8,WHITE);
        g.drawString(font,binding,x+10,y+h-13,MUTED);
        g.drawString(font,">",x+w-14,y+13,PURPLE);
    }
    public static void option(GuiGraphics g,Font font,int x,int y,int w,int h,String label,boolean active,boolean hovered) {
        g.fillGradient(x,y,x+w,y+h,active?0xFF215868:hovered?0xFF304257:0xFF243247,0xFF132031);
        g.fill(x,y,x+w,y+2,active?CYAN:hovered?PURPLE:0xFF304559);
        g.drawCenteredString(font,label,x+w/2,y+(h-8)/2,active?WHITE:MUTED);
    }
    public static void button(GuiGraphics g,Font font,int x,int y,int w,int h,String label,boolean hovered) { option(g,font,x,y,w,h,label,false,hovered); }
    public static void toggle(GuiGraphics g,Font font,int x,int y,int w,int h,String label,boolean active,boolean hovered) {moduleRow(g,font,x,y,w,h,label,active,null,hovered);}
}
