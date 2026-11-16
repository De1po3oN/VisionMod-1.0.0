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
        "ВИЗУАЛ", "HUD", "БОЙ", "МИР", "ДВИЖЕНИЕ", "УТИЛИТЫ", "FPS"
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
    private int modal = 0; // 0: cards; 1: ore settings; 2: advanced tools
    private static final String[] ORE_NAMES = {
        "DIAMOND", "DEBRIS", "IRON", "GOLD", "REDSTONE",
        "EMERALD", "LAPIS", "COPPER", "COAL", "QUARTZ"
    };
    private static final String[] ADVANCED_TOOLS = {
        "ORE COLORS", "PARTICLES", "CROSSHAIR", "PERFORMANCE", "CONFIGS"
    };
    private int captureBind = -1;
    private EditBox search;
    private EditBox customText;
    private List<Module> cachedModules = List.of();
    private String cachedQuery = null;
    private int cachedCategory = -1;
    private DashboardLayout layout;

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
        m.add(new Module(VISUAL,"Free Look","Hold Left Alt; release instantly restores view",
            ()->FreeLook.enabled,()->FreeLook.setEnabled(!FreeLook.enabled),
            ()->{FreeLook.smooth=!FreeLook.smooth;VisionExtras.save();}));
        extras(m,HUD,"CPS Counter","Mouse clicks per second",0);
        extras(m,HUD,"Armor HUD","Current armor equipment",1);
        extras(m,HUD,"Food + Sat","Hunger and saturation information",2);
        extras(m,HUD,"Crosshair Editor","Custom pixel-drawn crosshair",3);
        extras(m,HUD,"Shulker Summary","Item totals in shulker containers",9);
        extras(m,HUD,"Snowflakes","Configurable ambient snow",7);
        extras(m,HUD,"Air Hearts","Configurable floating heart particles",8);
        m.add(new Module(HUD,"Tab Heads","Player skin heads when holding TAB",
            ()->VisionTabHeads.enabled,VisionTabHeads::toggle,null));
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
        m.add(new Module(TOOLS,"Totem Animation","Компактная анимация активации тотема",()->VisionAdvanced.smallTotemAnimation,
            ()->{VisionAdvanced.smallTotemAnimation=!VisionAdvanced.smallTotemAnimation;VisionAdvanced.save();},VisionAdvanced::cycleTotemSize));
        m.add(new Module(TOOLS,"Shulker Grid","Предпросмотр 9 × 3 в инвентаре",()->VisionAdvanced.shulkerGrid,
            ()->{VisionAdvanced.shulkerGrid=!VisionAdvanced.shulkerGrid;VisionAdvanced.save();},null));
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
            i==0 ? VisionAppearance::cycleBrightness : i==1 ? VisionDashboardScreen::openOres
                : i==5 ? ()->net.minecraft.client.Minecraft.getInstance().setScreen(new ParticleSettingsScreen())
                : null));
    }

    private static void extras(List<Module> m,int category,String title,String info,int id) {
        Runnable options = switch(id) {
            case 3 -> ()->net.minecraft.client.Minecraft.getInstance().setScreen(new VisionCrosshairEditorScreen());
            case 4 -> VisionExtras::cycleZoom;
            case 6 -> ()->net.minecraft.client.Minecraft.getInstance().setScreen(new OreColorPickerScreen("Контур блока",()->VisionExtras.outlineColor,color->{VisionExtras.outlineColor=color;VisionExtras.save();},null));
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
                case 1 -> "Editable RGB gradient trail: RMB to customize";
                case 2 -> "Landing rings at the player's feet";
                default -> "Third-person local head decoration";
            },
            ()->VisionWorldEffects.enabled(id),()->VisionWorldEffects.toggle(id),
            id==1 ? ()->net.minecraft.client.Minecraft.getInstance().setScreen(new VisionTrailSettingsScreen()) : null));
    }

    private static void extended(List<Module> m,int category,int id) {
        m.add(new Module(category,VisionExtendedFeatures.NAMES[id],
            switch(id) {
                case 0 -> "Highlight selected blocks";
                case 1 -> "Client-observed defeat notification";
                case 2 -> "Dusk / warm / cool screen coloring";
                case 3 -> "Музыка из системного плеера · без расширений";
                case 4 -> "Animated solid pet with colors and wings";
                case 5 -> "Панель друзей поблизости · до 48 блоков";
                case 6 -> "Client hit-feedback sound";
                default -> "Filter ordinary weather particles";
            },
            ()->VisionExtendedFeatures.enabled(id),
            ()->VisionExtendedFeatures.toggle(id),
            id==6 ? VisionExtendedFeatures::cycleSound :
            id==3 ? ()->net.minecraft.client.Minecraft.getInstance().setScreen(new VisionLyricsScreen()) :
            id==2 ? VisionExtendedFeatures::cycleWorldColor :
                id==4 ? VisionExtendedFeatures::cyclePetColor : null));
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

    private static void openOres() {
        var screen = net.minecraft.client.Minecraft.getInstance().screen;
        if (screen instanceof VisionDashboardScreen dashboard) dashboard.modal = 1;
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
            case "Totem Animation" -> VisionHotkeys.totemAnimationToggle();
            case "Shulker Grid" -> VisionHotkeys.shulkerGridToggle();
            case "Real FPS Boost" -> VisionHotkeys.boostToggle();
            case "Adaptive Budgets" -> VisionHotkeys.performanceToggle(0);
            case "HUD Throttle" -> VisionHotkeys.performanceToggle(1);
            case "Ambient Throttle" -> VisionHotkeys.performanceToggle(2);
            default -> -1;
        };
    }


    @Override protected void init() {
        layout=DashboardLayout.of(width,height);
        w=layout.width();h=layout.height();sidebar=layout.sidebar();
        left=(width-w)/2;top=(height-h)/2;
        contentX=left+sidebar+22;contentW=layout.contentWidth();
        columns=layout.columns();rows=layout.rows();cardsPerPage=layout.cards();
        search=new EditBox(font,contentX,top+21,contentW,20,Component.literal("Поиск"));
        search.setMaxLength(48);search.setHint(Component.literal("Найти модуль…"));
        addRenderableWidget(search);
        customText=new EditBox(font,left+24,top+116,w-48,20,Component.literal("Друзья через запятую"));
        customText.setMaxLength(120);customText.visible=false;customText.active=false;
        addRenderableWidget(customText);
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta) {}
    private int accent(){return VisionMenuTheme.CYAN;}
    private static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
    private static void round(GuiGraphics g,int x,int y,int w,int h,int r,int color){
        if(w<=0||h<=0)return;
        r=Math.min(r,Math.min(w/2,h/2));
        g.fill(x+r,y,x+w-r,y+h,color);g.fill(x,y+r,x+w,y+h-r,color);
        for(int i=0;i<r;i++){
            int inset=(int)Math.ceil(r-Math.sqrt(r*r-(r-i-.5)*(r-i-.5)));
            g.fill(x+inset,y+i,x+w-inset,y+i+1,color);
            g.fill(x+inset,y+h-i-1,x+w-inset,y+h-i,color);
        }
    }
    private void textFit(GuiGraphics g,String text,int x,int y,int max,int color){
        if(max<1)return;
        if(font.width(text)>max)text=font.plainSubstrByWidth(text,Math.max(1,max-font.width("…")))+"…";
        g.drawString(font,text,x,y,color);
    }
    private void button(GuiGraphics g,int x,int y,int w,int h,String text,boolean selected,int mx,int my){
        boolean hover=inside(mx,my,x,y,w,h);
        round(g,x,y,w,h,5,selected?0xFF443A65:hover?0xFF33364E:0xFF25283C);
        if(selected)g.fill(x+6,y+h-2,x+w-6,y+h-1,accent());
        textFit(g,text,x+8,y+(h-8)/2,w-16,selected?WHITE:MUTED);
    }
    private List<Module> filtered(){
        String query=search.getValue().trim().toLowerCase(Locale.ROOT);
        if(cachedCategory==category&&query.equals(cachedQuery))return cachedModules;
        cachedModules=CATALOG.stream().filter(m->query.isEmpty()?m.category==category:
            (m.title+" "+m.info).toLowerCase(Locale.ROOT).contains(query)).toList();
        cachedCategory=category;cachedQuery=query;page=0;return cachedModules;
    }
    private int cardX(int i){return left+layout.cardLeft(i);}
    private int cardY(int i){return top+layout.cardTop(i);}
    private int cardWidth(){return layout.cardWidth();}
    private int maxPage(List<Module> m){return Math.max(0,(m.size()-1)/cardsPerPage);}

    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        g.fill(0,0,width,height,0x80080B15);
        round(g,left-4,top+6,w+8,h+3,12,0x6003040B);
        round(g,left-1,top-1,w+2,h+2,10,0xFF46405E);
        round(g,left,top,w,h,10,0xF5141725);
        g.fillGradient(left+14,top+1,left+w-14,top+3,accent(),VisionMenuTheme.PURPLE);
        g.drawString(font,"VISION",left+20,top+20,WHITE);
        g.drawString(font,"2.0 / BETA",left+20,top+35,accent());
        g.fill(left+16,top+59,left+w-16,top+60,0xFF303347);
        int step=Math.min(36,(h-108)/TABS.length);
        for(int i=0;i<TABS.length;i++)button(g,left+12,top+73+i*step,sidebar-4,step-3,
            TABS[i],category==i&&search.getValue().isBlank(),mx,my);
        button(g,left+12,top+h-29,sidebar-4,19,VisionModClient.enabled?"МОД: ВКЛ":"МОД: ВЫКЛ",VisionModClient.enabled,mx,my);
        List<Module> modules=filtered();page=Math.min(page,maxPage(modules));
        long active=CATALOG.stream().filter(m->m.enabled.getAsBoolean()).count();
        textFit(g,search.getValue().isBlank()?TABS[category]:"РЕЗУЛЬТАТЫ",contentX,top+72,contentW-76,WHITE);
        String counter=modules.size()+" / "+active+" вкл.";
        g.drawString(font,counter,contentX+contentW-font.width(counter),top+72,MUTED);
        for(int i=0;i<cardsPerPage&&page*cardsPerPage+i<modules.size();i++){
            Module m=modules.get(page*cardsPerPage+i);
            int x=cardX(i),y=cardY(i),cw=cardWidth();
            boolean on=m.enabled.getAsBoolean(),hover=inside(mx,my,x,y,cw,60);
            round(g,x,y,cw,60,6,hover?0xFF2E3048:0xFF202436);
            g.fill(x+9,y+12,x+11,y+28,on?accent():0xFF57596E);
            textFit(g,m.title,x+18,y+12,cw-56,WHITE);
            round(g,x+cw-32,y+11,22,12,6,on?0xFF68578E:0xFF43465B);
            round(g,x+cw-(on?20:29),y+14,6,6,3,on?accent():0xFFB2B1C5);
            textFit(g,m.info,x+12,y+32,cw-24,MUTED);
            int bind=bindingId(m);
            textFit(g,bind>=0?VisionHotkeys.label(bind):"ПКМ · подробнее",x+12,y+47,cw-24,0xFF858BA6);
        }
        if(modules.isEmpty())textFit(g,"Ничего не найдено. Попробуй другое название.",contentX,top+110,contentW,MUTED);
        int ny=top+h-48;
        button(g,contentX,ny,54,19,"<",page>0,mx,my);
        button(g,contentX+contentW-54,ny,54,19,">",page<maxPage(modules),mx,my);
        g.drawCenteredString(font,(page+1)+" / "+(maxPage(modules)+1),contentX+contentW/2,ny+5,MUTED);
        int bw=(contentW-10)/2;
        button(g,contentX,top+h-24,bw,17,"Настройки",false,mx,my);
        button(g,contentX+bw+10,top+h-24,bw,17,"Тема  •",false,mx,my);
        search.visible=details==null&&modal==0&&captureBind<0;
        search.active=search.visible;
        if(details!=null)renderDetails(g,mx,my);
        if(modal!=0)renderModal(g,mx,my);
        if(captureBind>=0){
            overlay(g,"ГОРЯЧАЯ КЛАВИША");
            textFit(g,"Нажми клавишу или кнопку мыши.",left+24,top+64,w-48,WHITE);
            textFit(g,"Esc — убрать привязку",left+24,top+85,w-48,MUTED);
        }
        super.render(g,mx,my,delta);
    }
    private void overlay(GuiGraphics g,String title){
        round(g,left+8,top+8,w-16,h-16,8,0xFF181C2D);
        g.drawString(font,title,left+24,top+25,accent());
        g.drawString(font,"×",left+w-30,top+24,MUTED);
        g.fill(left+24,top+44,left+w-24,top+45,0xFF36384F);
    }
    private void renderModal(GuiGraphics g,int mx,int my){
        overlay(g,modal==1?"РУДЫ · ПКМ: ЦВЕТ":"НАСТРОЙКИ");
        int gap=10,cw=(w-58)/2;
        if(modal==1){
            for(int i=0;i<ORE_NAMES.length;i++){
                int x=left+24+(i%2)*(cw+gap),y=top+56+(i/2)*28;
                button(g,x,y,cw,24,ORE_NAMES[i],OreSelection.enabled(i),mx,my);
                g.fill(x+cw-16,y+8,x+cw-8,y+16,OreColors.getColor(i));
            }
        }else{
            for(int i=0;i<ADVANCED_TOOLS.length;i++)button(g,left+24+(i%2)*(cw+gap),top+56+(i/2)*34,cw,28,ADVANCED_TOOLS[i],false,mx,my);
            if(h>238){
                button(g,left+24,top+168,w-48,24,"Вернуть позиции HUD",false,mx,my);
                textFit(g,"Открой чат: ЛКМ — двигать, ПКМ — сбросить.",left+24,top+201,w-48,MUTED);
            }
        }
    }
    private void openAdvancedTool(int i){
        switch(i){case 0->modal=1;case 1->minecraft.setScreen(new ParticleSettingsScreen());
            case 2->minecraft.setScreen(new VisionCrosshairEditorScreen());case 3->minecraft.setScreen(new PerformanceScreen());
            case 4->minecraft.setScreen(new ConfigsScreen());default->{}}
    }
    private void openDetails(Module module){
        details=module;search.setFocused(false);
        customText.visible=module.title.equals("Social Radar");customText.active=customText.visible;
        customText.setResponder(v->{});
        if(customText.visible){customText.setValue(VisionExtendedFeatures.friendNames());customText.setResponder(VisionExtendedFeatures::setFriendNames);}
    }
    private void closeDetails(){details=null;customText.visible=false;customText.active=false;customText.setFocused(false);}
    private void renderDetails(GuiGraphics g,int mx,int my){
        overlay(g,details.title.toUpperCase(Locale.ROOT));
        int y=top+59;
        String remaining=details.info;
        for(int i=0;i<2&&!remaining.isEmpty();i++){
            String part=font.plainSubstrByWidth(remaining,w-48);
            if(part.isEmpty())break;
            g.drawString(font,part,left+24,y+i*13,MUTED);remaining=remaining.substring(part.length());
        }
        String value=switch(details.title){
            case "Air Hearts","Snowflakes" -> "Режим: "+VisionExtras.ambientStyleLabel()+"  ·  Плотность: "+VisionExtras.ambientDensityLabel();
            case "Free Look" -> "Сглаживание: "+(FreeLook.smooth?"ВКЛ":"ВЫКЛ");
            case "Local Pet" -> VisionPetRenderer.suspended()?"Ошибка рендера · выключи и включи для повтора":"Цвет: "+VisionExtendedFeatures.petColorLabel();
            case "Trails","China Hat","Jump Circles","Target ESP" -> VisionWorldEffects.suspended()?"Ошибка рендера · выключи и включи для повтора":"Эффект виден в мире · F5: третье лицо";
            case "FullBright" -> "Яркость: "+VisionAppearance.brightness;
            case "Totem Animation" -> "Масштаб: "+VisionAdvanced.totemAnimationPercent+"%";
            case "Custom Models" -> VisionCustomModels.suspended()?"Ошибка модели · включи заново для повтора":"config/visionmod/custom-model.json";
            case "Custom Sound" -> "Звук: "+VisionExtendedFeatures.soundLabel();
            case "World Color" -> "Палитра: "+VisionExtendedFeatures.colorLabel();
            case "Hit Color" -> "Цвет: "+VisionTools2.hitColorName();
            case "World Lyrics" -> VisionMediaSession.state();
            case "Social Radar" -> "Друзья через запятую";
            default -> details.enabled.getAsBoolean()?"Модуль включён":"Модуль выключен";
        };
        textFit(g,value,left+24,top+97,w-48,WHITE);
        if(details.title.equals("Social Radar"))textFit(g,VisionExtendedFeatures.nearbyFriends(),left+24,top+142,w-48,accent());
        int bw=(w-58)/2,by=top+h-72;
        button(g,left+24,by,bw,26,details.enabled.getAsBoolean()?"Выключить":"Включить",details.enabled.getAsBoolean(),mx,my);
        if(details.options!=null)button(g,left+34+bw,by,bw,26,"Настроить",false,mx,my);
        if(details.title.equals("Air Hearts")||details.title.equals("Snowflakes"))button(g,left+24,top+h-39,w-48,22,"Изменить плотность",false,mx,my);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        double mx=e.x(),my=e.y();int b=e.button();
        if(captureBind>=0){VisionHotkeys.assign(captureBind,-b-1);captureBind=-1;return true;}
        if(details!=null||modal!=0){
            if(b==0&&inside(mx,my,left+w-43,top+12,30,28)){modal=0;closeDetails();return true;}
            if(details!=null){
                if(customText.visible&&inside(mx,my,customText.getX(),customText.getY(),customText.getWidth(),20))return super.mouseClicked(e,doubled);
                int bw=(w-58)/2;
                if(b==0){
                    if(inside(mx,my,left+24,top+h-72,bw,26))details.toggle.run();
                    else if(details.options!=null&&inside(mx,my,left+34+bw,top+h-72,bw,26))details.options.run();
                    else if((details.title.equals("Air Hearts")||details.title.equals("Snowflakes"))&&inside(mx,my,left+24,top+h-39,w-48,22))VisionExtras.cycleAmbientDensity();
                }
            }else{
                int cw=(w-58)/2;
                for(int i=0;i<(modal==1?10:ADVANCED_TOOLS.length);i++){
                    int x=left+24+(i%2)*(cw+10),y=top+56+(i/2)*(modal==1?28:34);
                    if(!inside(mx,my,x,y,cw,modal==1?24:28))continue;
                    if(modal==1){if(b==0)OreSelection.toggle(i);if(b==1)minecraft.setScreen(new OreColorPickerScreen(i));}
                    else if(b==0)openAdvancedTool(i);
                    return true;
                }
                if(modal==2&&h>238&&b==0&&inside(mx,my,left+24,top+168,w-48,24))VisionHudLayout.reset();
            }
            return true;
        }
        if(inside(mx,my,search.getX(),search.getY(),search.getWidth(),20))return super.mouseClicked(e,doubled);
        if(b==0){
            int step=Math.min(36,(h-108)/TABS.length);
            for(int i=0;i<TABS.length;i++)if(inside(mx,my,left+12,top+73+i*step,sidebar-4,step-3)){
                category=i;page=0;search.setValue("");search.setFocused(false);return true;
            }
            if(inside(mx,my,left+12,top+h-29,sidebar-4,19)){VisionModClient.enabled=!VisionModClient.enabled;return true;}
            int bw=(contentW-10)/2;
            if(inside(mx,my,contentX,top+h-24,bw,17)){modal=2;search.setFocused(false);return true;}
            if(inside(mx,my,contentX+bw+10,top+h-24,bw,17)){minecraft.setScreen(new VisionAppearanceScreen());return true;}
        }
        List<Module> modules=filtered();
        if(b==0){
            if(page>0&&inside(mx,my,contentX,top+h-48,54,19)){page--;return true;}
            if(page<maxPage(modules)&&inside(mx,my,contentX+contentW-54,top+h-48,54,19)){page++;return true;}
        }
        for(int i=0;i<cardsPerPage&&page*cardsPerPage+i<modules.size();i++)if(inside(mx,my,cardX(i),cardY(i),cardWidth(),60)){
            Module m=modules.get(page*cardsPerPage+i);
            search.setFocused(false);
            if(b==0)m.toggle.run();
            else if(b==1)openDetails(m);
            else if(b==2&&bindingId(m)>=0)captureBind=bindingId(m);
            return true;
        }
        return super.mouseClicked(e,doubled);
    }
    @Override public boolean keyPressed(KeyEvent e){
        if(captureBind>=0){VisionHotkeys.assign(captureBind,e.key()==GLFW.GLFW_KEY_ESCAPE?0:e.key());captureBind=-1;return true;}
        if(e.key()==GLFW.GLFW_KEY_ESCAPE&&(details!=null||modal!=0)){modal=0;closeDetails();return true;}
        if(details!=null||modal!=0){
            if(customText.visible)return super.keyPressed(e);
            return true;
        }
        return super.keyPressed(e);
    }
    @Override public boolean isPauseScreen(){return false;}
}
