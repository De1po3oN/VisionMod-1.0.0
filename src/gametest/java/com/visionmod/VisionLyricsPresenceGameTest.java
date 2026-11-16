package com.visionmod;

import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.phys.Vec3;
import static com.visionmod.GuiTestInput.check;

/** Real world text submissions and a real integrated-server presence handshake. */
final class VisionLyricsPresenceGameTest {
    static void run(ClientGameTestContext context,TestSingleplayerContext world)throws Exception{
        context.runOnClient(mc->{
            VisionModClient.enabled=true;VisionAppearance.playerBadge=true;
            if(!VisionExtendedFeatures.enabled(3))VisionExtendedFeatures.toggle(3);
            mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);
            VisionLyricsSource.clearMediaTrack();VisionExtendedFeatures.setCustomText("VISION · Текст перед камерой");
        });
        long before=context.computeOnClient(mc->VisionWorldLyrics.renderedFrames());
        context.waitTicks(1);
        context.runOnClient(mc->{
            check(VisionWorldLyrics.renderedFrames()>before,"First-person lyrics never submitted world text");
            check(VisionWorldLyrics.lastRenderedText().contains("перед камерой"),"Manual text did not appear immediately");
        });
        assertAnchor(context);VisualWorldGameTest.capture(context,"19-lyrics-world-first-person");
        for(CameraType type:new CameraType[]{CameraType.THIRD_PERSON_FRONT,CameraType.THIRD_PERSON_BACK}){
            context.runOnClient(mc->mc.options.setCameraType(type));context.waitTicks(2);assertAnchor(context);
            VisualWorldGameTest.capture(context,type==CameraType.THIRD_PERSON_FRONT?"20-lyrics-world-f5-front":"21-lyrics-world-f5-back");
        }
        context.runOnClient(mc->{
            VisionLyricsSource.receiveMediaTrack("Test","Artist","Synced song",1500,true);
            VisionLyricsSource.setMediaLyrics("Artist","Synced song","[00:01.00]FIRST TIMED LINE\n[00:02.00]SECOND TIMED LINE\n[00:03.00]");
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
        context.runOnClient(mc->{VisionModClient.enabled=true;mc.options.hideGui=false;});
        context.waitFor(mc->VisionPresenceClient.confirmed(mc.player.getUUID()),100);
        System.out.println("[VisionMod][LYRICS_PRESENCE] PASS: first-person/F5 world text, camera anchor, immediate timed seek, blank interval, server handshake/withdraw/restore, real name tag/TAB hooks and unknown-player exclusion");
    }
    private static void assertAnchor(ClientGameTestContext context){
        context.runOnClient(mc->{
            var camera=mc.gameRenderer.getMainCamera();
            var relative=VisionWorldLyrics.lastAnchor().subtract(camera.position());
            var forward=Vec3.directionFromRotation(camera.xRot(),camera.yRot());
            var up=Vec3.directionFromRotation(camera.xRot()-90,camera.yRot());
            var right=Vec3.directionFromRotation(0,camera.yRot()+90);
            check(Math.abs(relative.dot(right))<.01&&Math.abs(relative.dot(up)+.40)<.01&&Math.abs(relative.dot(forward)-2.4)<.01,"World text was not in front of the actual camera");
        });
    }
}
