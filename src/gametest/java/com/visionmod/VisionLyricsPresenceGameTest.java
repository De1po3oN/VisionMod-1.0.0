package com.visionmod;

import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import static com.visionmod.GuiTestInput.check;

/** Real world text submissions and a real integrated-server presence handshake. */
final class VisionLyricsPresenceGameTest {
    static void run(ClientGameTestContext context,TestSingleplayerContext world)throws Exception{
        context.runOnClient(mc->{
            // Keep the host's missing/real media session and HTTP lookup from replacing
            // our controlled, paused track. Native/HTTP integrations have separate tests.
            try{
                var stop=VisionMediaSession.class.getDeclaredMethod("stop");stop.setAccessible(true);stop.invoke(null);
                var retry=VisionMediaSession.class.getDeclaredField("retryAt");retry.setAccessible(true);retry.setLong(null,Long.MAX_VALUE);
            }catch(ReflectiveOperationException error){throw new AssertionError(error);}
            VisionModClient.enabled=true;VisionAppearance.playerBadge=true;
            if(!VisionExtendedFeatures.enabled(3))VisionExtendedFeatures.toggle(3);
            mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);
            VisionLyricsSource.clearMediaTrack();VisionExtendedFeatures.setCustomText("VISION · Текст перед камерой");
        });
        VisionWorldPlaneGameTest.run(context,world);
        context.runOnClient(mc->{
            VisionLyricsSource.receiveMediaTrack("Test","Artist","Synced song",1500,true);
            String fixture="[00:01.00]FIRST TIMED LINE\n[00:02.00]SECOND TIMED LINE\n[00:03.00]";
            try{
                var field=VisionLyricsLookup.class.getDeclaredField("CACHE");field.setAccessible(true);
                @SuppressWarnings("unchecked") var cache=(java.util.Map<String,String>)field.get(null);
                cache.put("Artist\nSynced song\n0",fixture);
            }catch(ReflectiveOperationException error){throw new AssertionError(error);}
            VisionLyricsSource.setMediaLyrics("Artist","Synced song",fixture);
        });context.waitTicks(1);
        context.runOnClient(mc->{
            check(VisionWorldLyrics.lastRenderedText().equals("FIRST TIMED LINE"),"Timed world line did not render");
            VisionLyricsSource.receiveMediaTrack("Test","Artist","Synced song",2500,true);
        });context.waitTicks(1);
        context.runOnClient(mc->{
            check(VisionWorldLyrics.lastRenderedText().equals("SECOND TIMED LINE"),"World text retained a stale line after seek");
            VisionLyricsSource.receiveMediaTrack("Test","Artist","Synced song",3500,true);
        });context.waitTicks(2);
        long blank=context.computeOnClient(mc->VisionWorldLyrics.renderedFrames());context.waitTicks(2);
        context.runOnClient(mc->check(blank==VisionWorldLyrics.renderedFrames(),"Instrumental gap submitted a placeholder world panel"));
        context.runOnClient(mc->{VisionLyricsSource.clearMediaTrack();VisionExtendedFeatures.setCustomText("");mc.options.hideGui=false;});
        context.waitFor(mc->VisionPresenceClient.confirmed(mc.player.getUUID()),100);
        context.runOnClient(mc->{
            UUID id=mc.player.getUUID();
            Component name=Component.literal("Player");
            check(VisionPresenceClient.decorate(id,name).getString().equals("Player  V"),"Confirmed player has no badge");
            check(name.getString().equals("Player"),"Badge mutated the original server nickname");
            check(VisionPresenceClient.decorate(UUID.randomUUID(),name)==name,"Unknown player incorrectly marked as a mod user");
            var tab=mc.gui.getTabList();var info=mc.getConnection().getPlayerInfo(id);
            check(info!=null&&tab.getNameForDisplay(info).getString().endsWith("  V"),"Real TAB hook omitted the badge");
            // Exercise the actual injected renderer with a nonlocal avatar carrying the confirmed identity.
            try{
                var avatar=new RemotePlayer(mc.level,mc.player.getGameProfile());
                avatar.setPos(mc.player.getX()+1,mc.player.getY(),mc.player.getZ()+2);
                var renderer=mc.getEntityRenderDispatcher().getRenderer(avatar);
                var method=renderer.getClass().getMethod("extractRenderState",Avatar.class,AvatarRenderState.class,float.class);
                var state=new AvatarRenderState();method.invoke(renderer,avatar,state,1f);
                check(state.nameTag!=null&&state.nameTag.getString().endsWith("  V"),"Real avatar name tag hook omitted the badge");
            }catch(ReflectiveOperationException error){throw new AssertionError(error);}
            VisionAppearance.playerBadge=false;
            check(VisionPresenceClient.decorate(id,name)==name,"Badge visibility switch was ignored");
        });
        context.waitFor(mc->!VisionPresenceClient.confirmed(mc.player.getUUID()),100);
        context.runOnClient(mc->VisionAppearance.playerBadge=true);
        context.waitFor(mc->VisionPresenceClient.confirmed(mc.player.getUUID()),100);
        context.runOnClient(mc->VisionModClient.enabled=false);
        context.waitFor(mc->!VisionPresenceClient.confirmed(mc.player.getUUID()),100);
        context.runOnClient(mc->{
            VisionModClient.enabled=true;mc.options.hideGui=false;
            try{var retry=VisionMediaSession.class.getDeclaredField("retryAt");retry.setAccessible(true);retry.setLong(null,0);}
            catch(ReflectiveOperationException error){throw new AssertionError(error);}
        });
        context.waitFor(mc->VisionPresenceClient.confirmed(mc.player.getUUID()),100);
        System.out.println("[VisionMod][LYRICS_PRESENCE] PASS: player-anchored world plane, immediate timed seek, blank interval, server handshake/withdraw/restore, real name tag/TAB hooks and unknown-player exclusion");
    }
}
