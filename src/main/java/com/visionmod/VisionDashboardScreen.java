package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Original Vision dashboard 2.0. Unlike the legacy screen this uses a searchable
 * two-column card grid with category navigation, details and real module actions.
 *
 * No external Fading files, UI resources or bytecode are used.
 * The dashboard deliberately reuses existing module toggles/settings so that
 * a visual refresh does not silently reset the user's configurations.
 */
public final class VisionDashboardScreen extends Screen {
    private record Module(int category, String title, String info,
                          BooleanSupplier enabled, Runnable toggle, Runnable options) { }

    private static final int VISUAL = 0;
    private static final int HUD = 1;
    private static final int COMBAT = 2;
    private static final int WORLD = 3;
    private static final int MOTION = 4;
    private static final int TOOLS = 5;
    private static final int PERFORMANCE = 6;
    private static final String[] TABS = {
        "VISUALS", "HUD", "COMBAT", "WORLD FX", "MOTION", "TOOLS", "PERFORMANCE"
    };
    private static final String[] SYMBOLS = {
        "V", "H", "C", "W", "M", "T", "F"
    };
    private static final int BG = 0xF40D101C;
    private static final int PANEL = 0xFF171827;
    private static final int CARD = 0xFF202235;
    private static final int HOVER = 0xFF302D49;
    private static final int WHITE = 0xFFF7F4FF;
    private static final int MUTED = 0xFFAAA9C2;
    private static final int BORDER = 0xFF464058;
    private static final int GREEN = 0xFF7BE0BE;
    private static final List<Module> CATALOG = catalog();

    private int category;
    private int page;
    private Module details;
    private int captureBind = -1;
    private EditBox search;
    private long pageAt = System.nanoTime();
    private final long openedAt = System.nanoTime();

    private int left, top, w, h, sidebar, contentX, contentW, columns, rows, cardsPerPage;

    public VisionDashboardScreen() {
        super(Component.literal("VISION 2.0"));
    }

    private static List<Module> catalog() {
        List<Module> m = new ArrayList<>();
        visual(m,"FullBright","Enhanced visibility in dark locations",0);
        visual(m,"Ore Highlight","Select ores and edit per-ore colors",1);
        visual(m,"Clear Water","Clear underwater fog",2);
        visual(m,"Clear Lava","Clear lava visibility",3);
        visual(m,"Player Hearts","Show health above other players",4);
        visual(m,"Particles","Manage block, portal and sprint particles",5);
        m.add(new Module(VISUAL,"Free Look","Hold Left Alt to orbit; F5 resets view",
            ()->FreeLook.enabled,()->FreeLook.enabled=!FreeLook.enabled,
            ()->{FreeLook.smooth=!FreeLook.smooth;VisionExtras.save();}));
        extras(m,HUD,"CPS Counter","Mouse clicks per second",0);
        extras(m,HUD,"Armor HUD","Current armor equipment",1);
        extras(m,HUD,"Food + Sat","Hunger and saturation information",2);
        extras(m,HUD,"Crosshair Editor","Custom pixel-drawn crosshair",3);
        extras(m,HUD,"Shulker Summary","Item totals in shulker containers",9);
        extras(m,HUD,"Snowflakes","Configurable ambient snow",7);
        extras(m,HUD,"Air Hearts","Configurable floating heart particles",8);
        extras(m,HUD,"Shulker Tooltip","Preview shulker items",15);
        feature(m,HUD,0); feature(m,HUD,1); feature(m,HUD,2);
        feature(m,HUD,4); feature(m,HUD,5);
        feature(m,COMBAT,3); feature(m,COMBAT,6); feature(m,COMBAT,7);
        world(m,COMBAT,0);
        tools(m,COMBAT,2);
        extended(m,COMBAT,1); extended(m,COMBAT,6);
        extras(m,WORLD,"Block Outline","Outline the block you target",6);
        extras(m,WORLD,"Low Fire","Compact on-screen fire",14);
        world(m,WORLD,1); world(m,WORLD,2); world(m,WORLD,3);
        extended(m,WORLD,0); extended(m,WORLD,2);
        extended(m,WORLD,3); extended(m,WORLD,4);
        extended(m,WORLD,7);
        extras(m,MOTION,"Zoom","Hold assigned zoom key",4);
        motion(m,MOTION,0); motion(m,MOTION,1); motion(m,MOTION,2);
        motion(m,MOTION,3); motion(m,MOTION,4);
        extras(m,TOOLS,"Fast Scroller","Inventory scrolling helper",5);
        tools(m,TOOLS,0); tools(m,TOOLS,1);
        extended(m,TOOLS,5);
        extras(m,TOOLS,"Arrow Hit Sound","Custom feedback for arrow impact",10);
        extras(m,TOOLS,"Small Sword","Compact held sword",11);
        extras(m,TOOLS,"Small Shield","Compact held shield",12);
        extras(m,TOOLS,"Small Totem","Compact held totem",13);
        m.add(new Module(PERFORMANCE,"Real FPS Boost",
            "Reversible vanilla render settings",
            ()->VisionRenderBoost.enabled,VisionRenderBoost::toggle,null));
        m.add(new Module(PERFORMANCE,"Adaptive Budgets",
            "Reduce selected rendering workloads under load",
            ()->VisionPerformance.adaptive,()->VisionHotkeys.togglePerformance(0),null));
        m.add(new Module(PERFORMANCE,"HUD Throttle",
            "Reduce expensive HUD updates at busy spawns",
            ()->VisionPerformance.limitHudOnBusySpawn,
            ()->VisionHotkeys.togglePerformance(1),null));
        m.add(new Module(PERFORMANCE,"Ambient Throttle",
            "Rate-limit background effects during lag",
            ()->VisionPerformance.reduceAmbientOnBusySpawn,
            ()->VisionHotkeys.togglePerformance(2),null));
        return List.copyOf(m);
    }

    private static void visual(List<Module> m,String name,String info,int i) {
        m.add(new Module(VISUAL,name,info,
            ()->switch(i) {
                case 0 -> VisionModClient.fullBright;
                case 1 -> VisionModClient.oreHighlight;
                case 2 -> VisionModClient.clearWater;
                case 3 -> VisionModClient.clearLava;
                case 4 -> VisionModClient.playerHearts;
                case 5 -> VisionModClient.particles;
                default -> false;
            },
            ()->{
                switch(i) {
                    case 0 -> VisionModClient.fullBright=!VisionModClient.fullBright;
                    case 1 -> VisionModClient.oreHighlight=!VisionModClient.oreHighlight;
                    case 2 -> VisionModClient.clearWater=!VisionModClient.clearWater;
                    case 3 -> VisionModClient.clearLava=!VisionModClient.clearLava;
                    case 4 -> VisionModClient.playerHearts=!VisionModClient.playerHearts;
                    case 5 -> VisionModClient.particles=!VisionModClient.particles;
                    default -> {}
                }
                VisionPerformance.custom();
            },
            i==1 ? ()->openLegacy(VisionScreen.Page.ORES)
                : i==5 ? ()->net.minecraft.client.Minecraft.getInstance().setScreen(new ParticleSettingsScreen())
                : null));
    }

    private static void extras(List<Module> m,int category,String title,String info,int id) {
        Runnable options = switch(id) {
            case 3 -> ()->net.minecraft.client.Minecraft.getInstance().setScreen(new VisionCrosshairEditorScreen());
            case 4 -> VisionExtras::cycleZoom;
            case 7,8 -> VisionExtras::cycleAmbientStyle;
            default -> null;
        };
        m.add(new Module(category,title,info,()->VisionExtras.enabled(id),
            ()->VisionExtras.toggle(id),options));
    }

    private static void feature(List<Module> m,int category,int id) {
        m.add(new Module(category,VisionFeaturePack.NAMES[id],
            switch(id) {
                case 0 -> "Cinematic letterbox framing";
                case 1 -> "Colored armor durability display";
                case 2 -> "Health bar on targeted entities";
                case 3 -> "Distance to target in meters";
                case 4 -> "Nearby primed TNT countdown";
                case 5 -> "Arrow name projected at arrow position";
                case 6 -> "Visual reaction to target hurt animation";
                default -> "Red edge effect on target hurt animation";
            },
            ()->VisionFeaturePack.enabled(id),()->VisionFeaturePack.toggle(id),null));
    }

    private static void world(List<Module> m,int category,int id) {
        m.add(new Module(category,VisionWorldEffects.NAMES[id],
            switch(id) {
                case 0 -> "Targeted living-entity outline";
                case 1 -> "Limited trail behind the player";
                case 2 -> "Landing rings at the player's feet";
                default -> "Third-person local head decoration";
            },
            ()->VisionWorldEffects.enabled(id),()->VisionWorldEffects.toggle(id),null));
    }

    private static void extended(List<Module> m,int category,int id) {
        m.add(new Module(category,VisionExtendedFeatures.NAMES[id],
            switch(id) {
                case 0 -> "Highlight selected blocks";
                case 1 -> "Client-observed defeat notification";
                case 2 -> "Dusk / warm / cool screen coloring";
                case 3 -> "Display your own floating text";
                case 4 -> "Little local companion beside player";
                case 5 -> "Nearby player names from local friends list";
                case 6 -> "Client hit-feedback sound";
                default -> "Filter ordinary weather particles";
            },
            ()->VisionExtendedFeatures.enabled(id),
            ()->VisionExtendedFeatures.toggle(id),
            id==2 ? VisionExtendedFeatures::cycleWorldColor : null));
    }

    private static void motion(List<Module> m,int category,int id) {
        m.add(new Module(category,VisionMotionEffects.NAMES[id],
            switch(id) {
                case 0 -> "Alternative first-person swing pose";
                case 1 -> "Optional dropped-item pose rotation";
                case 2 -> "Local particle feedback when carrying a spear";
                case 3 -> "Local visual spear contact effect";
                default -> "Lightweight camera-motion accent, not true blur";
            },
            ()->VisionMotionEffects.selected(id),
            ()->VisionMotionEffects.toggle(id),null));
    }

    private static void tools(List<Module> m,int category,int id) {
        m.add(new Module(category,VisionTools2.NAMES[id],
            switch(id) {
                case 0 -> "Rate-limited vanilla item use";
                case 1 -> "Local JSON box model on player";
                default -> "Choose color of hit and target outline";
            },
            ()->VisionTools2.selected(id),()->VisionTools2.toggle(id),
            id==2 ? VisionTools2::cycleHitColor : null));
    }

    private static void openLegacy(VisionScreen.Page page) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new VisionScreen(page));
    }

    /** Keep all original hotkey IDs, never renumber older player configs. */
    private static int bindingId(Module m) {
        return switch(m.title) {
            case "FullBright" -> VisionHotkeys.visual(0);
            case "Ore Highlight" -> VisionHotkeys.visual(1);
            case "Clear Water" -> VisionHotkeys.visual(2);
            case "Clear Lava" -> VisionHotkeys.visual(3);
            case "Player Hearts" -> VisionHotkeys.visual(4);
            case "Particles" -> VisionHotkeys.visual(5);
            case "Free Look" -> VisionHotkeys.visual(6);
            case "CPS Counter" -> VisionHotkeys.extra(0);
            case "Armor HUD" -> VisionHotkeys.extra(1);
            case "Food + Sat" -> VisionHotkeys.extra(2);
            case "Crosshair Editor" -> VisionHotkeys.extra(3);
            case "Zoom" -> VisionHotkeys.extra(4);
            case "Fast Scroller" -> VisionHotkeys.extra(5);
            case "Block Outline" -> VisionHotkeys.extra(6);
            case "Snowflakes" -> VisionHotkeys.extra(7);
            case "Air Hearts" -> VisionHotkeys.extra(8);
            case "Shulker Summary" -> VisionHotkeys.extra(9);
            case "Arrow Hit Sound" -> VisionHotkeys.extra(10);
            case "Small Sword" -> VisionHotkeys.extra(11);
            case "Small Shield" -> VisionHotkeys.extra(12);
            case "Small Totem" -> VisionHotkeys.extra(13);
            case "Low Fire" -> VisionHotkeys.extra(14);
            case "Shulker Tooltip" -> VisionHotkeys.extra(15);
            case "Real FPS Boost" -> VisionHotkeys.boostToggle();
            case "Adaptive Budgets" -> VisionHotkeys.performanceToggle(0);
            case "HUD Throttle" -> VisionHotkeys.performanceToggle(1);
            case "Ambient Throttle" -> VisionHotkeys.performanceToggle(2);
            default -> -1;
        };
    }

    @Override
    protected void init() {
        w = Math.max(300,Math.min(628,width - 16));
        h = Math.max(215,Math.min(390,height - 14));
        left = (width - w)/2;
        top = (height - h)/2;
        sidebar = w >= 470 ? 136 : 102;
        contentX = left + sidebar + 19;
        contentW = w - sidebar - 36;
        columns = contentW >= 335 ? 2 : 1;
        rows = Math.max(1,Math.min(3,(h - 146)/71));
        cardsPerPage = columns * rows;
        search = new EditBox(font, left + w - Math.min(198,contentW) - 16,
            top + 35,Math.min(184,contentW - 12),20,Component.literal("Search modules"));
        search.setMaxLength(48);
        search.setHint(Component.literal("Find a module..."));
        addRenderableWidget(search);
        page = 0;
    }

    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta) {
        // World stays visible; no heavy post-processing blur or render-target copy.
    }

    private int accent() { return VisionMenuTheme.CYAN; }
    private static void round(GuiGraphics g,int x,int y,int rw,int rh,int radius,int color) {
        if (rw<=0 || rh<=0) return;
        radius = Math.min(radius,Math.min(rw/2,rh/2));
        g.fill(x+radius,y,x+rw-radius,y+rh,color);
        g.fill(x,y+radius,x+rw,y+rh-radius,color);
        for(int i=0;i<radius;i++) {
            int inset = (int)Math.ceil(radius-Math.sqrt(radius*radius-(radius-i-.5)*(radius-i-.5)));
            g.fill(x+inset,y+i,x+rw-inset,y+i+1,color);
            g.fill(x+inset,y+rh-i-1,x+rw-inset,y+rh-i,color);
        }
    }

    private void textFit(GuiGraphics g,String value,int x,int y,int maxWidth,int color) {
        String text = value;
        while (!text.isEmpty() && font.width(text)>maxWidth)
            text = text.substring(0,text.length()-1);
        if (!text.equals(value) && text.length()>3)
            text=text.substring(0,text.length()-3)+"...";
        g.drawString(font,text,x,y,color);
    }

    private List<Module> filtered() {
        String q=search==null?"":search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Module> result=new ArrayList<>();
        for(Module m:CATALOG) {
            if(q.isEmpty() && m.category!=category) continue;
            if(!q.isEmpty() &&
                !m.title.toLowerCase(Locale.ROOT).contains(q) &&
                !m.info.toLowerCase(Locale.ROOT).contains(q)) continue;
            result.add(m);
        }
        return result;
    }

    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        long elapsed = System.nanoTime()-openedAt;
        float arrival = ease((float)(elapsed/340_000_000.0));
        float pageChange = ease((float)((System.nanoTime()-pageAt)/250_000_000.0));
        g.fill(0,0,width,height,((int)(0x96*arrival)<<24)|0x00070812);
        round(g,left-5,top+7,w+10,h+9,13,0x50040610);
        round(g,left-1,top-1,w+2,h+2,12,BORDER);
        round(g,left,top,w,h,12,BG);
        // Prominent glass header and its thin accent line.
        round(g,left+9,top+8,w-18,52,9,0xFF25243C);
        g.fillGradient(left+20,top+57,left+w-20,top+59,accent(),VisionMenuTheme.PURPLE);
        round(g,left+17,top+19,28,27,7,accent());
        g.drawCenteredString(font,"V",left+31,top+28,0xFF151326);
        g.drawString(font,"VISION",left+53,top+19,WHITE);
        g.drawString(font,"VISUAL CLIENT  /  2.0.0",left+53,top+34,MUTED);
        // Compact sidebar categories.
        round(g,left+10,top+67,sidebar-8,h-113,9,0xFF17192A);
        int tabTop=top+76,tabStep=Math.max(23,Math.min(37,(h-138)/TABS.length));
        for(int i=0;i<TABS.length;i++){
            int ty=tabTop+i*tabStep;
            boolean on=i==category && search.getValue().isBlank();
            boolean hover=inside(mx,my,left+17,ty,sidebar-21,tabStep-4);
            if(on||hover)round(g,left+16,ty,sidebar-20,tabStep-4,6,on?0xFF383052:0xFF28283F);
            if(on)g.fill(left+16,ty+4,left+19,ty+tabStep-8,accent());
            g.drawString(font,SYMBOLS[i],left+24,ty+9,on?accent():MUTED);
            textFit(g,TABS[i],left+41,ty+9,sidebar-61,on?WHITE:MUTED);
        }
        // Bottom navigation remains in one place across all categories.
        g.fill(left+12,top+h-42,left+w-12,top+h-41,0xFF3F3A57);
        g.drawString(font,"END  toggle all   |   RMB  details",left+19,top+h-30,MUTED);
        g.drawString(font,"OPEN BETA",left+20,top+h-17,accent());

        List<Module> modules = filtered();
        int maxPage=Math.max(0,(modules.size()-1)/Math.max(1,cardsPerPage));
        if(page>maxPage)page=maxPage;
        String heading=search.getValue().isBlank()?TABS[category]:"SEARCH RESULTS";
        g.drawString(font,heading,contentX,top+75,WHITE);
        g.drawString(font,modules.size()+" MODULES",contentX,top+89,MUTED);
        int first=page*cardsPerPage;
        int gap=11,cardW=(contentW-(columns-1)*gap)/columns;
        int cardH=63;
        for(int i=0;i<cardsPerPage && first+i<modules.size();i++) {
            Module m=modules.get(first+i);
            int cx=contentX+(i%columns)*(cardW+gap);
            int cy=top+112+(i/columns)*71;
            boolean hovered=inside(mx,my,cx,cy,cardW,cardH);
            boolean enabled=m.enabled.getAsBoolean();
            int slide=(int)(8*(1-pageChange));
            round(g,cx+slide,cy,cardW,cardH,7,hovered?HOVER:CARD);
            g.fill(cx+slide+1,cy+9,cx+slide+4,cy+cardH-9,enabled?accent():0xFF52516B);
            textFit(g,m.title,cx+slide+13,cy+11,cardW-64,WHITE);
            textFit(g,m.info,cx+slide+13,cy+30,cardW-23,MUTED);
            g.fill(cx+slide+13,cy+45,cx+slide+cardW-13,cy+46,0xFF3C3954);
            int binding=bindingId(m);
            if(binding>=0)textFit(g,VisionHotkeys.label(binding),cx+slide+13,cy+51,
                cardW-25,0xFF8988A6);
            else g.drawString(font,"LMB toggle  /  RMB details",cx+slide+13,cy+51,0xFF8988A6);
            round(g,cx+slide+cardW-40,cy+10,28,15,7,enabled?0xFF544679:0xFF44445B);
            round(g,cx+slide+cardW-(enabled?27:37),cy+13,9,9,4,enabled?accent():0xFFBBB7CD);
        }
        if(modules.isEmpty())g.drawCenteredString(font,"Nothing found. Try another search.",contentX+contentW/2,top+164,MUTED);

        // Lightweight paging controls; no scroll event mixins or large lists.
        int navY=top+h-60;
        round(g,contentX,navY,54,16,5,page>0?0xFF39334F:0xFF252637);
        round(g,contentX+contentW-54,navY,54,16,5,page<maxPage?0xFF39334F:0xFF252637);
        g.drawCenteredString(font,"PREV",contentX+27,navY+4,page>0?WHITE:MUTED);
        g.drawCenteredString(font,"NEXT",contentX+contentW-27,navY+4,page<maxPage?WHITE:MUTED);
        g.drawCenteredString(font,(page+1)+" / "+(maxPage+1),contentX+contentW/2,navY+4,MUTED);

        // Distinct controls to reach existing rich editors, without removing them.
        int advX=left+w-101;
        round(g,advX,top+h-34,85,23,6,0xFF403552);
        g.drawCenteredString(font,"ADVANCED",advX+42,top+h-26,WHITE);

        if(details!=null)renderDetails(g,mx,my);
        if(captureBind>=0) {
            int ox=left+w/2-147,oy=top+h/2-44;
            round(g,ox-3,oy-3,300,87,9,0xFF655383);
            round(g,ox,oy,294,81,8,0xFF19182D);
            g.drawCenteredString(font,"PRESS A KEY TO BIND",ox+147,oy+18,WHITE);
            g.drawCenteredString(font,"ESC clears / Mouse buttons also work",ox+147,oy+40,MUTED);
        }
        super.render(g,mx,my,delta);
    }

    private void renderDetails(GuiGraphics g,int mx,int my) {
        int x=contentX-5,y=top+103,ww=contentW+10,hh=h-168;
        round(g,x-3,y-3,ww+6,hh+6,9,0xFF58506F);
        round(g,x,y,ww,hh,8,0xFF18182B);
        g.drawString(font,"MODULE / DETAILS",x+16,y+16,accent());
        textFit(g,details.title,x+16,y+36,ww-29,WHITE);
        textFit(g,details.info,x+16,y+55,ww-28,MUTED);
        g.drawString(font,details.enabled.getAsBoolean()?"STATUS: ENABLED":"STATUS: DISABLED",
            x+16,y+78,details.enabled.getAsBoolean()?GREEN:MUTED);
        int actionY=y+hh-37;
        round(g,x+13,actionY,Math.min(115,(ww-45)/2),27,6,0xFF42345F);
        g.drawString(font,"TOGGLE",x+28,actionY+10,WHITE);
        if(details.options!=null){
            round(g,x+131,actionY,Math.min(118,ww-145),27,6,0xFF303950);
            g.drawString(font,"OPTIONS",x+149,actionY+10,WHITE);
        }
        g.drawString(font,"[x]",x+ww-24,y+16,MUTED);
    }

    private static boolean inside(double mx,double my,int x,int y,int w,int h) {
        return mx>=x && my>=y && mx<x+w && my<y+h;
    }
    private static float ease(float p){
        p=Math.min(1f,Math.max(0f,p));
        return 1f-(1f-p)*(1f-p)*(1f-p);
    }

    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled) {
        double mx=e.x(),my=e.y();
        int button=e.button();
        if(captureBind>=0) {
            VisionHotkeys.assign(captureBind,-button-1);
            captureBind=-1;
            return true;
        }
        // Let native EditBox claim search input first.
        if(inside(mx,my,search.getX(),search.getY(),search.getWidth(),search.getHeight()))
            return super.mouseClicked(e,doubled);
        if(details!=null) {
            int x=contentX-5,y=top+103,ww=contentW+10,hh=h-168;
            if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if(inside(mx,my,x+ww-38,y+6,33,29)){details=null;return true;}
                if(inside(mx,my,x+13,y+hh-37,Math.min(115,(ww-45)/2),27)){
                    details.toggle.run();return true;
                }
                if(details.options!=null &&
                    inside(mx,my,x+131,y+hh-37,Math.min(118,ww-145),27)){
                    details.options.run();return true;
                }
            }
            return true;
        }
        if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT &&
            inside(mx,my,left+w-101,top+h-34,85,23)) {
            openLegacy(VisionScreen.Page.VISUALS);return true;
        }
        int tabTop=top+76,tabStep=Math.max(23,Math.min(37,(h-138)/TABS.length));
        if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for(int i=0;i<TABS.length;i++){
                if(inside(mx,my,left+17,tabTop+i*tabStep,sidebar-21,tabStep-4)){
                    category=i;page=0;pageAt=System.nanoTime();search.setValue("");
                    search.setFocused(false);return true;
                }
            }
        }
        List<Module> modules=filtered();
        int maxPage=Math.max(0,(modules.size()-1)/Math.max(1,cardsPerPage));
        int navY=top+h-60;
        if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT){
            if(page>0 && inside(mx,my,contentX,navY,54,16)){
                page--;pageAt=System.nanoTime();return true;
            }
            if(page<maxPage && inside(mx,my,contentX+contentW-54,navY,54,16)){
                page++;pageAt=System.nanoTime();return true;
            }
        }
        int first=page*cardsPerPage;
        int gap=11,cardW=(contentW-(columns-1)*gap)/columns;
        for(int i=0;i<cardsPerPage && first+i<modules.size();i++){
            int x=contentX+(i%columns)*(cardW+gap);
            int y=top+112+(i/columns)*71;
            if(!inside(mx,my,x,y,cardW,63))continue;
            Module m=modules.get(first+i);
            if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                m.toggle.run();return true;
            }
            if(button==GLFW.GLFW_MOUSE_BUTTON_MIDDLE && bindingId(m)>=0) {
                captureBind=bindingId(m);return true;
            }
            if(button==GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                details=m;return true;
            }
        }
        return super.mouseClicked(e,doubled);
    }

    @Override public boolean keyPressed(KeyEvent e) {
        if(captureBind>=0) {
            VisionHotkeys.assign(captureBind,e.key()==GLFW.GLFW_KEY_ESCAPE?0:e.key());
            captureBind=-1;
            return true;
        }
        if(details!=null && e.key()==GLFW.GLFW_KEY_ESCAPE){details=null;return true;}
        return super.keyPressed(e);
    }
    @Override public boolean isPauseScreen(){return false;}
}
