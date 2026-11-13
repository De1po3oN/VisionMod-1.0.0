package com.visionmod;

import net.fabricmc.api.ModInitializer;

/** Register sound resources in the common initialization phase. */
public final class VisionMod implements ModInitializer {
    @Override
    public void onInitialize() {
        VisionSounds.init();
    }
}
