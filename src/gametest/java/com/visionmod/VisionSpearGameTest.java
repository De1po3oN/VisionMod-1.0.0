package com.visionmod;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import static com.visionmod.GuiTestInput.check;

/** Exercises actual vanilla mouse attacks, lunge impulse and server damage packets. */
final class VisionSpearGameTest {
    static void run(ClientGameTestContext context, TestSingleplayerContext world)throws Exception {
        context.setScreen(()->null);
        world.getServer().runCommand("tp @p 8 -60 0 0 0");
        world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:diamond_spear");
        context.runOnClient(mc->{
            check(VisionFonts.width(mc.font,"iiiiWWWW Vision 2026")!=mc.font.width("iiiiWWWW Vision 2026"),"Inter font was not loaded; UI fell back to Minecraft's font");
            mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=true;
            mc.setWindowActive(true);mc.mouseHandler.grabMouse();
            if(!VisionMotionEffects.selected(2))VisionMotionEffects.toggle(2);
            if(!VisionMotionEffects.selected(3))VisionMotionEffects.toggle(3);
            VisionSpearEffects.reset();
        });context.waitTicks(8);
        long beforeLunge=context.computeOnClient(mc->VisionSpearEffects.lungeEvents());
        context.getInput().holdKey(options->options.keyUp);context.waitTicks(5);
        context.getInput().releaseKey(options->options.keyUp);context.waitTicks(3);
        context.runOnClient(mc->check(VisionSpearEffects.lungeEvents()==beforeLunge,"Walking while holding a spear emitted a fake lunge"));
        world.getServer().runCommand("tp @p 8 -60 0 0 0");
        world.getServer().runCommand("summon minecraft:husk 8 -60 3 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"vision_spear_target\"]}");
        context.waitTicks(8);
        long beforeHit=context.computeOnClient(mc->VisionSpearEffects.pierceEvents());
        context.getInput().pressMouse(0);context.waitTicks(3);
        context.runOnClient(mc->check(VisionSpearEffects.pierceEvents()>beforeHit,"Actual spear attack did not produce a damage-attributed pierce effect"));
        VisualWorldGameTest.capture(context,"29-spear-pierce");
        world.getServer().runCommand("kill @e[tag=vision_spear_target]");
        world.getServer().runCommand("enchant @p minecraft:lunge 3");
        world.getServer().runCommand("tp @p 8 -60 0 0 0");
        context.waitTicks(22);
        context.runOnClient(mc->mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        long beforeFrames=context.computeOnClient(mc->VisionSpearEffects.renderedFrames());
        context.getInput().pressMouse(0);context.waitTicks(2);
        context.runOnClient(mc->{
            check(VisionSpearEffects.lungeEvents()>beforeLunge,"Vanilla Lunge III did not produce a lunge effect");
            check(VisionSpearEffects.renderedFrames()>beforeFrames,"Spear effects were never submitted in the world");
        });
        VisualWorldGameTest.capture(context,"30-spear-lunge");
        context.runOnClient(mc->VisionModClient.enabled=false);context.waitTicks(1);
        context.runOnClient(mc->{
            check(VisionSpearEffects.activeBursts()==0,"Global OFF retained spear geometry");
            VisionModClient.enabled=true;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;
        });
        System.out.println("[VisionMod][SPEAR] PASS: no walking false positive, actual mouse hit + attributed server packet, vanilla Lunge III impulse, world frames, global OFF cleanup");
    }
}
