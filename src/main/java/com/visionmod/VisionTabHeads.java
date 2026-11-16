package com.visionmod;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Optional TAB heads side panel, showing genuine cached player skin head
 * textures without downloading skins separately. Bounded to 12 entries.
 * The vanilla TAB list is never replaced or modified.
 */
public final class VisionTabHeads {
    private static final Path CONFIG=FabricLoader.getInstance().getConfigDir()
        .resolve("visionmod/tab-heads.properties");
    public static boolean enabled=false;
    private VisionTabHeads(){}

    public static void init(){
        load();
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(
            "visionmod","tab_heads"),(g,tick)->{
            Minecraft mc=Minecraft.getInstance();
            if(!VisionModClient.enabled || !enabled || mc.player==null ||
                mc.getConnection()==null || !VisionHudLayout.hudVisible(mc) ||
                (!mc.options.keyPlayerList.isDown()&&!VisionHudLayout.chatOpen(mc)))return;
            // TAB is only open on key hold. Draw a compact supplementary panel
            // left of the native tab list; never alter its HUD/layout.
            var infos=mc.getConnection().getOnlinePlayers();
            int max=Math.min(12,infos.size());
            if(max==0)return;
            var pos=VisionHudLayout.place(VisionHudLayout.Widget.TAB,
                8,26,118,22+max*16);
            int x=pos.x(),y=pos.y(),w=pos.width(),h=pos.height();
            VisionHudTheme.panel(g,x,y,w,h,VisionMenuTheme.PURPLE);
            g.drawString(mc.font,"ИГРОКИ  "+infos.size(),x+7,y+7,VisionMenuTheme.CYAN);
            int index=0;
            for(PlayerInfo info:infos){
                if(index>=max)break;
                int py=y+19+index*16;
                PlayerFaceRenderer.draw(g,info.getSkin(),x+6,py,13);
                String name=info.getProfile().name();
                while(!name.isEmpty()&&mc.font.width(name)>w-27)
                    name=name.substring(0,name.length()-1);
                g.drawString(mc.font,name,x+24,py+3,0xFFF2F4FC);
                index++;
            }
        });
    }
    public static void toggle(){enabled=!enabled;save();}
    public static void load(){
        if(!Files.isRegularFile(CONFIG))return;
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(CONFIG)){
            p.load(in);enabled=Boolean.parseBoolean(p.getProperty("enabled","false"));
        }catch(Exception e){System.err.println("[VisionMod] Tab Heads config: "+e);}
    }
    public static void save(){
        Properties p=new Properties();p.setProperty("enabled",Boolean.toString(enabled));
        try{Files.createDirectories(CONFIG.getParent());
            try(OutputStream out=Files.newOutputStream(CONFIG)){p.store(out,"TAB heads overlay");}
        }catch(Exception e){System.err.println("[VisionMod] Tab Heads save: "+e);}
    }
}
