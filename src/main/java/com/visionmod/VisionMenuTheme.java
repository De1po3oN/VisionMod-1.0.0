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
    public static final String VERSION = "1.1.0";
    private static final int[] PRIMARY = {0xFFA89CFF, 0xFFF38CC9, 0xFF7EE2BE, 0xFFF3BB7A, 0xFF7DADEB, 0xFFECECFD};
    private static final int[] SECONDARY = {0xFFDB9BFF, 0xFFB6A7FF, 0xFF91B7FC, 0xFFFF90B0, 0xFF84E4E8, 0xFFB6A5D3};
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("visionmod/theme.properties");
    private static int palette = 0;
    public static int CYAN = PRIMARY[0];
    public static int PURPLE = SECONDARY[0];
    public static final int WHITE = 0xFFF2F0FC;
    public static final int MUTED = 0xFFA6A7C2;
    public static final int GREEN = 0xFF63EDB0;
    public static final int RED = 0xFFF97995;
    public static final int BACK = 0xF5101220;
    public static final int SURFACE = 0xFF222538;
    public static final int HOVER = 0xFF31334F;

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
        // Original low-cost graphite/lavender style. No borrowed textures/fonts.
        g.fill(x-15,y+12,r+15,b+17,0x38050415);
        g.fill(x-8,y+5,r+8,b+9,0x71090817);
        g.fill(x-2,y-2,r+2,b+2,0xFF39354C);
        g.fillGradient(x,y,r,b,0xFA202033,0xFA10131F);
        g.fillGradient(x+1,y+1,r-1,y+53,0xFF29273E,0xFF1E2133);
        g.fillGradient(x+16,y+2,r-16,y+4,CYAN,PURPLE);
        g.fill(x+1,y+54,r-1,y+55,0xFF494159);
        g.fillGradient(x+1,b-34,r-1,b-33,0xFF665A86,0xFF4A426C);
        // Subtle vertical light rail and geometric corners: distinctive from the reference.
        g.fillGradient(x+9,y+66,x+11,b-43,CYAN,0xFF403557);
        g.fill(x+2,y+2,x+8,y+8,0xFF453B65);
        g.fill(r-9,b-9,r-2,b-2,PURPLE);
        g.fill(x+14,y+13,x+17,y+41,CYAN);
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
        g.drawString(font,"ACCENT",r-157,y+27,0xFFA3A0BF);
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
        g.fillGradient(x+11,y+62,x+119,y+268,0xFA1B1C2E,0xFA141626);
        g.fill(x+118,y+62,x+119,y+268,0xFF454056);
        for(int i=0;i<tabs.length;i++) {
            int bx=x+18,by=y+70+i*32;
            boolean on=i==active,hover=inside(mx,my,bx,by,93,27);
            if(on || hover) {
                g.fillGradient(bx,by,bx+93,by+25,on?0xFF454064:0xFF35354C,0xFF222238);
            }
            g.fill(bx,by+4,bx+(on?3:1),by+23,on?CYAN:0xFF65617F);
            g.drawString(font,on?Component.literal(tabs[i]).withStyle(ChatFormatting.BOLD):Component.literal(tabs[i]),
                bx+12,by+10,on?WHITE: (hover?WHITE:MUTED));
            if(on)g.fill(bx+10,by+25,bx+84,by+28,CYAN);
        }
        g.drawString(font,"OPEN BETA • 1.1",x+23,y+257,0xFF918BAB);
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

    private static String fit(Font font, String source, int width) {
        if (source == null) return "";
        if (font.width(source) <= width) return source;
        String value = source;
        while (!value.isEmpty() && font.width(value + "...") > width) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "...";
    }

    public static void moduleRow(GuiGraphics g,Font font,int x,int y,int w,int h,String label,boolean enabled,String binding,boolean hovered) {
        g.fill(x+2,y+3,x+w+2,y+h+3,0x700A0915);
        g.fillGradient(x,y,x+w,y+h,hovered?0xFF38344F:0xFF2A293D,0xFF1B1D2D);
        g.fill(x,y,x+w,y+1,enabled?0xFF8A78D1:0xFF49475F);
        g.fill(x,y,x+3,y+h,enabled?CYAN:0xFF514D67);
        g.drawString(font,fit(font,label,w-52),x+10,y+8,WHITE);
        if(binding != null)g.drawString(font,fit(font,binding,w-19),x+10,y+h-13,MUTED);
        // Stepped-corner switch avoids additional textures, shader passes and allocations.
        int px=x+w-39,py=y+12;
        g.fill(px,py+2,px+31,py+13,enabled?0xFF504A80:0xFF4C4A5C);
        g.fill(px+2,py,px+29,py+15,enabled?0xFF504A80:0xFF4C4A5C);
        int thumb=px+(enabled?17:3);
        g.fill(thumb,py+3,thumb+11,py+12,enabled?CYAN:0xFF9C9AB0);
    }
    public static void actionRow(GuiGraphics g,Font font,int x,int y,int w,int h,String label,String binding,boolean hovered) {
        g.fillGradient(x,y,x+w,y+h,hovered?0xFF3B3654:SURFACE,0xFF1B1B2E);
        g.fill(x,y,x+3,y+h,PURPLE);
        g.drawString(font,fit(font,label,w-28),x+10,y+8,WHITE);
        g.drawString(font,fit(font,binding,w-28),x+10,y+h-13,MUTED);
        g.drawString(font,">",x+w-14,y+13,PURPLE);
    }
    public static void option(GuiGraphics g,Font font,int x,int y,int w,int h,String label,boolean active,boolean hovered) {
        g.fillGradient(x,y,x+w,y+h,active?0xFF4F4976:hovered?0xFF393750:0xFF302C44,0xFF1C1A2B);
        g.fill(x,y,x+w,y+2,active?CYAN:hovered?PURPLE:0xFF4B435E);
        g.drawCenteredString(font,label,x+w/2,y+(h-8)/2,active?WHITE:MUTED);
    }
    public static void button(GuiGraphics g,Font font,int x,int y,int w,int h,String label,boolean hovered) { option(g,font,x,y,w,h,label,false,hovered); }
    public static void toggle(GuiGraphics g,Font font,int x,int y,int w,int h,String label,boolean active,boolean hovered) {moduleRow(g,font,x,y,w,h,label,active,null,hovered);}
}
