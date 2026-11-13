package com.visionmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * Client-only passive sign text throttling for sign-heavy spawn areas.
 *
 * Vanilla must still render signs every frame. We retain the previously
 * extracted SignText for up to 10 seconds on distant signs, avoiding repeated
 * text shaping when the server sends frequent text updates. Close signs always
 * display changes immediately. Their actual block entities and server data
 * are never changed or delayed.
 */
public final class VisionSignOptimizer {
    private static final long TEXT_REFRESH_NANOS = 10_000_000_000L;
    private static final double LIVE_TEXT_DISTANCE_SQR = 12.0 * 12.0;
    private static final double OUTLINE_DISTANCE_SQR = 16.0 * 16.0;

    // Weak keys prevent unloaded signs from being retained by the cache.
    private static final WeakHashMap<SignBlockEntity, CachedText> TEXT_CACHE = new WeakHashMap<>();
    private static WeakReference<ClientLevel> lastLevel = new WeakReference<>(null);

    private VisionSignOptimizer() { }

    public static void apply(SignBlockEntity sign, SignRenderState state, Vec3 cameraPos) {
        if (!VisionModClient.enabled || cameraPos == null || state.frontText == null || state.backText == null) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            TEXT_CACHE.clear();
            lastLevel = new WeakReference<>(null);
            return;
        }
        if (lastLevel.get() != level) {
            TEXT_CACHE.clear();
            lastLevel = new WeakReference<>(level);
        }

        BlockPos pos = sign.getBlockPos();
        double distanceSqr = cameraPos.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);

        // Never delay editing or signs close enough to interact with.
        if (distanceSqr <= LIVE_TEXT_DISTANCE_SQR) {
            TEXT_CACHE.remove(sign);
            return;
        }

        long now = System.nanoTime();
        CachedText cached = TEXT_CACHE.get(sign);
        if (cached == null
                || now < cached.updatedAtNanos
                || now - cached.updatedAtNanos >= TEXT_REFRESH_NANOS
                || appearanceChanged(cached.front, state.frontText)
                || appearanceChanged(cached.back, state.backText)) {
            // A new text object is installed only at the next scheduled refresh,
            // or immediately if the dye / glow appearance was changed.
            TEXT_CACHE.put(sign, new CachedText(state.frontText, state.backText, now));
        } else {
            state.frontText = cached.front;
            state.backText = cached.back;
        }

        // Glowing text remains readable. Skip the extra expensive outline
        // passes at a distance; the sign model and its text still render.
        if (distanceSqr > OUTLINE_DISTANCE_SQR) {
            state.renderTextOutline = false;
        }
    }

    private static boolean appearanceChanged(SignText oldText, SignText newText) {
        return oldText.isGlowing() != newText.isGlowing()
                || oldText.getColor() != newText.getColor();
    }

    private record CachedText(SignText front, SignText back, long updatedAtNanos) { }
}
