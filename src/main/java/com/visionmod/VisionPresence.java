package com.visionmod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Optional in-server presence relay. No chat, web service or persistent player database. */
public final class VisionPresence {
    public record Hello(boolean visible) implements CustomPacketPayload {
        public static final Type<Hello> TYPE=new Type<>(Identifier.fromNamespaceAndPath("visionmod","presence_hello_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Hello> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,Hello::visible,Hello::new);
        @Override public Type<Hello> type(){return TYPE;}
    }
    public record Peer(UUID player,boolean visible) implements CustomPacketPayload {
        public static final Type<Peer> TYPE=new Type<>(Identifier.fromNamespaceAndPath("visionmod","presence_peer_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Peer> CODEC=StreamCodec.composite(UUIDUtil.STREAM_CODEC,Peer::player,ByteBufCodecs.BOOL,Peer::visible,Peer::new);
        @Override public Type<Peer> type(){return TYPE;}
    }
    private static final Map<MinecraftServer,Set<UUID>> MEMBERS=new HashMap<>();
    private VisionPresence(){}
    public static void init(){
        PayloadTypeRegistry.playC2S().register(Hello.TYPE,Hello.CODEC);
        PayloadTypeRegistry.playS2C().register(Peer.TYPE,Peer.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Hello.TYPE,(payload,context)->{
            ServerPlayer player=context.player();MinecraftServer server=context.server();
            if(!ServerPlayNetworking.canSend(player,Peer.TYPE))return;
            var members=MEMBERS.computeIfAbsent(server,s->new HashSet<>());
            if(payload.visible()){
                if(!members.add(player.getUUID()))return;
                // Include the sender's own confirmation; identity comes from the connection.
                for(UUID id:members)ServerPlayNetworking.send(player,new Peer(id,true));
                broadcast(server,new Peer(player.getUUID(),true),player.getUUID());
            }else if(members.remove(player.getUUID()))broadcast(server,new Peer(player.getUUID(),false),null);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{
            var members=MEMBERS.get(server);
            if(members!=null&&members.remove(handler.player.getUUID()))broadcast(server,new Peer(handler.player.getUUID(),false),null);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(MEMBERS::remove);
    }
    private static void broadcast(MinecraftServer server,Peer payload,UUID except){
        for(ServerPlayer player:server.getPlayerList().getPlayers())
            if(!player.getUUID().equals(except)&&ServerPlayNetworking.canSend(player,Peer.TYPE))ServerPlayNetworking.send(player,payload);
    }
}
