package com.visionmod.mixin;

import com.visionmod.FreeLook;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Replace the camera's initial yaw and pitch at the vanilla call site.
 * This runs before third-person camera offset and wall collision, so the
 * camera orbits around the player rather than rotating in place.
 * Vanilla F5 changes and first-person camera remain untouched.
 */
@Mixin(Camera.class)
public abstract class FreeLookCameraMixin {
    @ModifyArgs(
        method = "setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
            ordinal = 0
        ),
        // This hook is essential. A missing injection must fail CI's real
        // Minecraft startup rather than silently ship a broken Free Look.
        require = 1
    )
    private void visionmod$orbit(Args args, Level area, Entity entity,
                                  boolean thirdPerson, boolean reverse, float partialTick) {
        // Camera setup runs every rendered frame. The mouse-release state
        // must be synchronized even when the player does not move the mouse.
        FreeLook.syncFromMouse();
        if (!FreeLook.isOrbitCamera() || !thirdPerson || reverse) return;
        FreeLook.stepCamera();
        FreeLook.cameraHookApplied();
        args.set(0, FreeLook.cameraYaw());
        args.set(1, FreeLook.cameraPitch());
    }
}
