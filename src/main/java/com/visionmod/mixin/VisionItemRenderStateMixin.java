package com.visionmod.mixin;

import com.visionmod.VisionItemPhysicsState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public abstract class VisionItemRenderStateMixin implements VisionItemPhysicsState {
    @Unique private boolean visionmod$grounded;
    @Unique private double visionmod$movementSpeed;
    @Override public void visionmod$setMotion(boolean grounded,double speed) {
        visionmod$grounded=grounded;
        visionmod$movementSpeed=speed;
    }
    @Override public boolean visionmod$isGrounded(){return visionmod$grounded;}
    @Override public double visionmod$speed(){return visionmod$movementSpeed;}
}
