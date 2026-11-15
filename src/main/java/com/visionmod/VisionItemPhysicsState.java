package com.visionmod;

/** Client-side item render-state metadata. No server physics changes. */
public interface VisionItemPhysicsState {
    void visionmod$setMotion(boolean grounded, double speed);
    boolean visionmod$isGrounded();
    double visionmod$speed();
}
