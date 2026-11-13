package com.visionmod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Original generated placeholder audio. Replace the OGG in resource packs if desired. */
public final class VisionSounds {
    public static final Identifier ARROW_ID = Identifier.fromNamespaceAndPath("visionmod", "arrow_hit_phrase");
    public static final SoundEvent ARROW_HIT = SoundEvent.createVariableRangeEvent(ARROW_ID);
    private VisionSounds() { }
    public static void init() { Registry.register(BuiltInRegistries.SOUND_EVENT, ARROW_ID, ARROW_HIT); }
}
