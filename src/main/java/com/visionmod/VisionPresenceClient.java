package com.visionmod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Badges are based on server-relayed declarations, never guessed from names or skins. */
public final class VisionPresenceClient {
    private static final Set<UUID> PEERS=new HashSet<>();
    private static Boolean published;
    private static final Component BADGE=Component.literal("  V").withColor(0x8AE8ED);
    private VisionPresenceClient(){}
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(VisionPresence.Peer.TYPE,(payload,context)->{
            if(payload.visible()){if(PEERS.size()<4096)PEERS.add(payload.player());}
            else PEERS.remove(payload.player());
        });
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->reset());
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(mc.player==null||mc.getConnection()==null||!ClientPlayNetworking.canSend(VisionPresence.Hello.TYPE))return;
            boolean visible=VisionModClient.enabled&&VisionAppearance.playerBadge;
            if(published==null||published!=visible){ClientPlayNetworking.send(new VisionPresence.Hello(visible));published=visible;}
        });
    }
    private static void reset(){PEERS.clear();published=null;}
    public static boolean confirmed(UUID id){return PEERS.contains(id);}
    public static boolean marked(UUID id){
        if(!VisionModClient.enabled||!VisionAppearance.playerBadge||id==null)return false;
        var mc=Minecraft.getInstance();
        return PEERS.contains(id)||(mc.player!=null&&id.equals(mc.player.getUUID()));
    }
    public static Component decorate(UUID id,Component name){
        return name!=null&&marked(id)?name.copy().append(BADGE):name;
    }
    public static String status(){
        if(!VisionAppearance.playerBadge)return "Метка и объявление мода выключены";
        var mc=Minecraft.getInstance();
        return mc.getConnection()!=null&&ClientPlayNetworking.canSend(VisionPresence.Hello.TYPE)?
            "Обмен метками через сервер":"Для чужих меток нужен VisionMod на сервере";
    }
}
