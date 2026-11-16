package com.visionmod;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Exercises the real Minecraft key/mouse pipeline and rendered Camera, not just feature flags. */
final class VisionControlsGameTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static void run(ClientGameTestContext context)throws java.io.IOException{
        context.runOnClient(mc->{
            VisionModClient.enabled=true;FreeLook.setEnabled(true);FreeLook.smooth=false;
            VisionHotkeys.assign(VisionHotkeys.visual(6),GLFW.GLFW_KEY_LEFT_ALT);
            mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setWindowActive(true);mc.mouseHandler.grabMouse();
        });
        context.getInput().moveCursor(1,0);context.waitTicks(2);
        float[] facing=context.computeOnClient(mc->new float[]{mc.player.getYRot(),mc.player.getXRot()});
        context.getInput().holdAlt();context.waitTicks(2);
        context.runOnClient(mc->check(FreeLook.active&&mc.options.getCameraType()==CameraType.THIRD_PERSON_BACK,"Alt did not start Free Look"));
        context.getInput().moveCursor(120,24);context.waitTicks(3);
        context.runOnClient(mc->{
            check(Math.abs(Mth.wrapDegrees(mc.gameRenderer.getMainCamera().yRot()-facing[0]))>3,"Actual camera did not orbit after mouse movement");
            check(Math.abs(Mth.wrapDegrees(mc.player.getYRot()-facing[0]))<.01&&Math.abs(mc.player.getXRot()-facing[1])<.01,"Free Look rotated the player");
            check(mc.gameRenderer.getMainCamera().position().distanceTo(mc.player.getEyePosition())>.5,"Camera rotated in place instead of orbiting");
        });
        VisualWorldGameTest.capture(context,"07-free-look-orbit");
        context.getInput().releaseAlt();context.waitTicks(2);
        context.runOnClient(mc->check(!FreeLook.active&&mc.options.getCameraType()==CameraType.FIRST_PERSON,"Alt release did not restore first person"));
        context.runOnClient(mc->mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        context.getInput().holdAlt();context.waitTicks(2);context.getInput().releaseAlt();context.waitTicks(2);
        context.runOnClient(mc->check(mc.options.getCameraType()==CameraType.THIRD_PERSON_FRONT,"Original front perspective was lost"));
        context.runOnClient(mc->mc.options.setCameraType(CameraType.FIRST_PERSON));
        context.getInput().holdAlt();context.waitTicks(2);
        context.getInput().pressKey(options->options.keyTogglePerspective);context.waitTicks(2);
        context.runOnClient(mc->check(!FreeLook.active&&mc.options.getCameraType()==CameraType.THIRD_PERSON_FRONT,"F5 did not override held Free Look"));
        context.waitTicks(2);context.runOnClient(mc->check(!FreeLook.active,"Free Look reactivated before Alt release"));
        context.getInput().releaseAlt();context.waitTicks(1);
        context.runOnClient(mc->{mc.options.setCameraType(CameraType.FIRST_PERSON);VisionHotkeys.assign(VisionHotkeys.visual(6),GLFW.GLFW_KEY_K);});
        context.getInput().holdKey(GLFW.GLFW_KEY_K);context.waitTicks(2);
        context.runOnClient(mc->check(FreeLook.active,"Rebound Free Look key did not work"));
        context.setScreen(VisionDashboardScreen::new);context.waitTicks(2);
        context.runOnClient(mc->check(!FreeLook.active,"Opening a menu left the camera captured"));
        context.getInput().releaseKey(GLFW.GLFW_KEY_K);context.setScreen(()->null);
        context.runOnClient(mc->{
            VisionHotkeys.assign(VisionHotkeys.visual(6),GLFW.GLFW_KEY_LEFT_ALT);FreeLook.smooth=true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);VisionAppearance.animations=VisionAppearance.smoothHotbar=true;
            mc.player.getInventory().setSelectedSlot(0);
        });
        context.waitTicks(4);
        context.runOnClient(mc->check(VisionHotbarAnimation.frames()>0,"Vanilla hotbar animation hook never ran"));
        context.getInput().pressKey(options->options.keyHotbarSlots[6]);
        context.runOnClient(mc->check(mc.player.getInventory().getSelectedSlot()==6,"Animation delayed the actual selected item"));
        VisualWorldGameTest.capture(context,"08-hotbar-transition");
        context.waitTicks(6);
        context.runOnClient(mc->check(Math.abs(VisionHotbarAnimation.position()-120)<.05,"Hotbar did not settle on the selected item"));
        VisualWorldGameTest.capture(context,"09-hotbar-settled");
        context.runOnClient(mc->{VisionAppearance.smoothHotbar=false;mc.player.getInventory().setSelectedSlot(2);});
        context.waitTicks(2);
        context.runOnClient(mc->{check(VisionHotbarAnimation.position()==40,"Hotbar OFF did not restore vanilla position");VisionAppearance.smoothHotbar=true;});
        System.out.println("[VisionMod][CONTROLS] PASS: actual Alt/mouse orbit, unchanged player aim, release/front/F5/menu/rebind and rendered hotbar/key selection/OFF");
    }
}
