package com.visionmod;

import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import static com.visionmod.GuiTestInput.check;

/** World model regression: same world point from different cameras, perspective, terrain depth. */
final class VisionWorldPlaneGameTest {
    static void run(ClientGameTestContext context,TestSingleplayerContext world)throws Exception{
        world.getServer().runCommand("tp @p 8 -60 0 0 0");
        context.runOnClient(mc->{
            mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=true;
            mc.setWindowActive(true);mc.mouseHandler.grabMouse();
            FreeLook.setEnabled(true);FreeLook.smooth=false;
            VisionExtendedFeatures.setCustomText("");
        });
        context.getInput().moveCursor(1,0);context.waitTicks(12);
        long before=context.computeOnClient(mc->VisionWorldLyrics.renderedFrames());
        context.runOnClient(mc->VisionExtendedFeatures.setCustomText("Текст в мире"));context.waitTicks(1);
        context.runOnClient(mc->{
            check(VisionWorldLyrics.renderedFrames()>before&&VisionWorldLyrics.lastRenderedText().equals("Текст в мире"),"World text did not appear on the next rendered frame");
            var delta=VisionWorldLyrics.lastAnchor().subtract(mc.player.position());
            var forward=Vec3.directionFromRotation(0,mc.player.getYRot());
            check(Math.abs(delta.dot(forward)-WorldLyricMotion.DISTANCE)<.06&&Math.abs(delta.y-WorldLyricMotion.HEIGHT)<.06,"Text is not anchored ahead of the player in world coordinates");
        });
        VisualWorldGameTest.capture(context,"19-lyrics-world-first-person");
        int close=glyphPixels("19-lyrics-world-first-person");
        check(close>100,"World model produced no visible glyphs: "+close);
        Vec3 anchor=context.computeOnClient(mc->VisionWorldLyrics.lastAnchor());
        for(CameraType type:new CameraType[]{CameraType.THIRD_PERSON_FRONT,CameraType.THIRD_PERSON_BACK}){
            context.runOnClient(mc->{
                mc.options.setCameraType(type);
                if(type==CameraType.THIRD_PERSON_BACK){mc.player.setXRot(30);mc.player.xRotO=30;}
            });context.waitTicks(3);
            assertFixed(context,anchor);
            VisualWorldGameTest.capture(context,type==CameraType.THIRD_PERSON_FRONT?"20-lyrics-world-f5-front":"21-lyrics-world-f5-back");
        }
        int far=glyphPixels("21-lyrics-world-f5-back");
        check(far>20&&close>far*1.5,"Text did not show physical perspective: close="+close+", far="+far);
        float body=context.computeOnClient(mc->mc.player.getYRot());
        context.getInput().holdAlt();context.waitTicks(2);
        context.getInput().moveCursor(450,24);context.waitTicks(4);assertFixed(context,anchor);
        context.runOnClient(mc->{
            check(Math.abs(Mth.wrapDegrees(mc.gameRenderer.getMainCamera().yRot()-body))>10,"Camera orbit fixture did not rotate");
            check(Math.abs(Mth.wrapDegrees(mc.player.getYRot()-body))<.01,"Camera orbit changed the player's model direction");
        });
        VisualWorldGameTest.capture(context,"22-lyrics-world-oblique");
        context.getInput().releaseAlt();context.waitTicks(4);
        context.runOnClient(mc->{mc.options.setCameraType(CameraType.FIRST_PERSON);mc.player.setXRot(0);mc.player.xRotO=0;});context.waitTicks(3);
        long wallFrames=context.computeOnClient(mc->VisionWorldLyrics.renderedFrames());
        world.getServer().runCommand("fill 5 -61 1 12 -55 2 minecraft:stone");
        world.getClientWorld().waitForChunksRender();context.waitTicks(6);assertFixed(context,anchor);
        VisualWorldGameTest.capture(context,"23-lyrics-world-wall-occlusion");
        int hidden=glyphPixels("23-lyrics-world-wall-occlusion");
        check(hidden<Math.max(40,close/10),"World text was drawn through an opaque wall: "+hidden+" vs "+close);
        context.runOnClient(mc->check(VisionWorldLyrics.renderedFrames()>wallFrames,"Wall test disabled the model instead of using depth testing"));
        world.getServer().runCommand("fill 5 -61 1 12 -55 2 minecraft:air");
        world.getClientWorld().waitForChunksRender();context.waitTicks(6);
        VisualWorldGameTest.capture(context,"24-lyrics-world-wall-removed");
        check(glyphPixels("24-lyrics-world-wall-removed")>100,"Text did not reappear after the wall was removed");
        context.runOnClient(mc->FreeLook.smooth=true);
        System.out.println("[VisionMod][WORLD_PLANE] PASS: fixed player-relative world position across F5/Free Look, perspective glyph sizes "+close+"/"+far+", wall occlusion "+hidden+", removal restores visible text");
    }
    private static void assertFixed(ClientGameTestContext context,Vec3 anchor){
        context.runOnClient(mc->check(VisionWorldLyrics.lastAnchor().distanceTo(anchor)<.02,"Turning or switching the camera moved the text model in the world"));
    }
    private static int glyphPixels(String name)throws Exception{
        var image=ImageIO.read(Path.of("vision-world-captures",name+".png").toFile());int pixels=0;
        for(int y=image.getHeight()/5;y<image.getHeight()*85/100;y++)for(int x=image.getWidth()/8;x<image.getWidth()*7/8;x++){
            int c=image.getRGB(x,y),r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
            if(r>225&&g>225&&b>235&&b-r>=4)pixels++;
        }
        return pixels;
    }
}
