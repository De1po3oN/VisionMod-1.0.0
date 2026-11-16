package com.visionmod;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import static com.visionmod.GuiTestInput.check;

/** Actual render poses, camera-only orbit, jumping, both F5 views and a colliding camera. */
final class VisionPetMotionGameTest {
    static void run(ClientGameTestContext context,TestSingleplayerContext world)throws Exception{
        context.runOnClient(mc->{
            VisionAppearance.animations=true;mc.options.hideGui=true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.setWindowActive(true);mc.mouseHandler.grabMouse();
            FreeLook.setEnabled(true);FreeLook.smooth=false;
            VisionExtendedFeatures.setCustomText("");VisionLyricsSource.clearMediaTrack();
            try{
                var current=VisionWorldLyrics.class.getDeclaredMethod("current",Minecraft.class);current.setAccessible(true);
                check(((String)current.invoke(null,mc)).isEmpty(),"No music produced a floating placeholder panel");
            }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        });
        context.getInput().moveCursor(1,0);context.waitTicks(10);
        float playerYaw=context.computeOnClient(mc->mc.player.getYRot());
        context.getInput().holdAlt();context.waitTicks(2);
        context.getInput().moveCursor(120,0);context.waitTicks(1);
        context.runOnClient(mc->{
            PetMotion.Pose pose=VisionPetRenderer.lastPose();check(pose!=null,"Pet did not update its render pose");
            double angle=Math.toDegrees(Math.atan2(pose.z()-mc.player.getZ(),pose.x()-mc.player.getX()));
            double lag=Math.abs(Mth.wrapDegrees((float)(mc.gameRenderer.getMainCamera().yRot()-angle)));
            check(lag>.1&&lag<40,"Camera motion did not create a small following delay");
            check(Math.abs(Mth.wrapDegrees(mc.player.getYRot()-playerYaw))<.01,"Fixture rotated the body instead of only the camera");
        });
        VisualWorldGameTest.capture(context,"12-pet-camera-lag");
        context.waitTicks(14);assertSide(context);
        VisualWorldGameTest.capture(context,"13-pet-camera-settled");
        context.getInput().releaseAlt();context.waitTicks(14);assertSide(context);
        world.getServer().runCommand("tp @p 8 -60 0 0 10");context.waitTicks(8);
        double baseline=context.computeOnClient(mc->VisionPetRenderer.lastPose().y());
        context.getInput().holdKey(options->options.keyJump);context.waitTicks(2);
        context.getInput().releaseKey(options->options.keyJump);context.waitTicks(1);
        context.runOnClient(mc->{
            var pose=VisionPetRenderer.lastPose();
            check(mc.player.getY()>-59.8&&pose.y()>baseline+.01&&pose.y()<mc.player.getY()+1.30,"Pet did not rise smoothly behind the player's jump");
        });
        VisualWorldGameTest.capture(context,"14-pet-jump-rise");
        context.waitFor(mc->mc.player.onGround(),100);context.waitTicks(1);
        context.runOnClient(mc->check(VisionPetRenderer.lastPose().y()>mc.player.getY()+1.48,"Pet did not retain smooth downward inertia at landing"));
        VisualWorldGameTest.capture(context,"15-pet-jump-landing");
        context.waitTicks(18);
        context.runOnClient(mc->check(Math.abs(VisionPetRenderer.lastPose().y()-mc.player.getY()-1.4)<.07,"Pet did not return to shoulder height"));
        long before=VisionCustomModels.renderedFrames();
        context.runOnClient(mc->mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));context.waitTicks(5);assertSide(context);
        check(VisionCustomModels.renderedFrames()>before,"Body model was disabled instead of correctly attached");
        VisualWorldGameTest.capture(context,"16-f5-front-no-floating-box");
        context.runOnClient(mc->mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));context.waitTicks(5);assertSide(context);
        VisualWorldGameTest.capture(context,"17-f5-back-body-attachment");
        world.getServer().runCommand("fill 7 -60 -1 10 -58 -1 minecraft:stone");
        world.getClientWorld().waitForChunksRender();
        context.waitFor(mc->mc.gameRenderer.getMainCamera().position().distanceToSqr(mc.player.getEyePosition())<.85*.85,100);
        context.waitTicks(5);
        long[] closeFrames=context.computeOnClient(mc->new long[]{VisionPetRenderer.renderedFrames(),VisionCustomModels.renderedFrames()});
        context.waitTicks(3);
        context.runOnClient(mc->{
            check(VisionPetRenderer.renderedFrames()==closeFrames[0],"Pet geometry entered a camera pressed against the head");
            check(VisionCustomModels.renderedFrames()==closeFrames[1],"Body model entered a camera pressed against the head");
            check(!VisionPetRenderer.suspended()&&!VisionCustomModels.suspended(),"Clearance check caused a renderer exception");
        });
        VisualWorldGameTest.capture(context,"18-f5-wall-camera-clearance");
        world.getServer().runCommand("fill 7 -60 -1 10 -58 -1 minecraft:air");
        world.getClientWorld().waitForChunksRender();context.waitTicks(5);
        context.runOnClient(mc->FreeLook.smooth=true);
        System.out.println("[VisionMod][PET_MOTION] PASS: camera-only lag/settle/return, jump ascent/descent/landing, F5 front/back, head-camera clearance and no empty lyric panel");
    }
    private static void assertSide(ClientGameTestContext context){
        context.runOnClient(mc->{
            var pose=VisionPetRenderer.lastPose();check(pose!=null,"Missing pet pose");
            double x=pose.x()-mc.player.getX(),z=pose.z()-mc.player.getZ();
            float angle=(float)Math.toDegrees(Math.atan2(z,x));
            check(Math.abs(Mth.wrapDegrees(mc.gameRenderer.getMainCamera().yRot()-angle))<1.5,"Pet did not settle relative to the actual camera");
            check(Math.abs(Math.sqrt(x*x+z*z)-1.04)<.02,"Pet crossed the player's body instead of following a side anchor");
        });
    }
}
