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
        require = 0
    )
    private void visionmod$orbit(Args args, Level area, Entity entity,
                                  boolean thirdPerson, boolean reverse, float partialTick) {
        if (!FreeLook.isOrbitCamera() || !thirdPerson || reverse) return;
        args.set(0, FreeLook.yaw);
        args.set(1, FreeLook.pitch);
    }
}
