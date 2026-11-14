package com.visionmod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Original snow-like ambient particles. Both Air Hearts and Snowflakes use the
 * same world-space drift and camera-facing translucent sprite renderer.
 *
 * No vanilla HEART/SNOWFLAKE particle textures or factories, no shader copy,
 * no network packets, and no per-frame entity/world scans. Hard cap: 96 sprites.
 */
public final class VisionAmbientParticles {
    private static final Identifier HEART = Identifier.fromNamespaceAndPath(
        "visionmod", "textures/particle/soft_heart.png");
    private static final Identifier SNOW = Identifier.fromNamespaceAndPath(
        "visionmod", "textures/particle/soft_snow.png");

    private static final int CAPACITY = 96;
    private static final Random RANDOM = new Random();
    private static final Sprite[] ACTIVE = new Sprite[CAPACITY];
    private static int count;
    private static int spawnTicks;
    private static ClientLevel world;
    private static long lastTickNanos;

    public static final int HEART_KIND = 0;
    public static final int SNOW_KIND = 1;

    private static final class Sprite {
        int type, age, lifetime, color;
        double x, y, z, prevX, prevY, prevZ;
        double dx, dy, dz, phase;
        float size, spin;
    }

    private VisionAmbientParticles() { }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(VisionAmbientParticles::tick);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(ctx -> {
            if (!VisionModClient.enabled || ctx.consumers() == null ||
                ctx.matrices() == null || count == 0) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || mc.level != world) return;
            Vec3 camera = mc.gameRenderer.getMainCamera().position();
            var cameraRotation = mc.gameRenderer.getMainCamera().rotation();
            PoseStack pose = ctx.matrices();
            VertexConsumer hearts = ctx.consumers().getBuffer(RenderTypes.entityTranslucent(HEART));
            VertexConsumer snow = ctx.consumers().getBuffer(RenderTypes.entityTranslucent(SNOW));

            // Interpolate between client ticks for smooth motion at any FPS.
            float partial = Math.min(1.0f, Math.max(0.0f,
                (System.nanoTime() - lastTickNanos) / 50_000_000.0f));
            for (int i = 0; i < count; i++) {
                Sprite p = ACTIVE[i];
                if ((p.type == HEART_KIND && !VisionExtras.hearts) ||
                    (p.type == SNOW_KIND && !VisionExtras.snow)) continue;

                double x = p.prevX + (p.x - p.prevX) * partial - camera.x;
                double y = p.prevY + (p.y - p.prevY) * partial - camera.y;
                double z = p.prevZ + (p.z - p.prevZ) * partial - camera.z;
                double distanceSq = x*x + y*y + z*z;
                if (distanceSq > 32 * 32 || distanceSq < 0.20) continue;

                float life = (p.age + partial) / (float)p.lifetime;
                float fadeIn = Math.min(1.0f, (p.age + partial + 1.0f) / 8.0f);
                float fadeOut = Math.min(1.0f, (p.lifetime - p.age - partial) / 17.0f);
                float opacity = Math.min(fadeIn, fadeOut);
                if (opacity <= 0.0f) continue;
                float shimmer = 0.88f + 0.12f * (float)Math.sin(p.phase + (p.age + partial)*0.16);
                int alpha = Math.min(208, Math.max(0, Math.round(196f * opacity * shimmer)));
                if (alpha < 3) continue;

                float size = p.size * (0.95f + 0.06f * (float)Math.sin(
                    p.phase + (p.age + partial) * 0.09f));
                pose.pushPose();
                pose.translate(x, y, z);
                pose.mulPose(cameraRotation);
                pose.mulPose(Axis.ZP.rotationDegrees(
                    p.spin * (p.age + partial) + (float)Math.sin(p.phase + p.age*.07f)*3f));
                int color = (alpha << 24) | (p.color & 0x00ffffff);
                VertexConsumer output = p.type == HEART_KIND ? hearts : snow;
                VisionGeometry.quad(output, pose,
                    -size, -size, 0f,
                     size, -size, 0f,
                     size,  size, 0f,
                    -size,  size, 0f,
                    color, 0f, 0f, 1f);
                pose.popPose();
            }
        });
    }

    public static void clearType(int type) {
        for (int i = count - 1; i >= 0; i--) {
            if (ACTIVE[i].type == type) removeAt(i);
        }
    }

    private static void clear() {
        for (int i = 0; i < count; i++) ACTIVE[i] = null;
        count = 0;
        spawnTicks = 0;
    }

    private static void removeAt(int index) {
        ACTIVE[index] = ACTIVE[--count];
        ACTIVE[count] = null;
    }

    private static void tick(Minecraft mc) {
        lastTickNanos = System.nanoTime();
        if (mc.level == null || mc.player == null) {
            clear();
            world = null;
            return;
        }
        if (world != mc.level) {
            clear();
            world = mc.level;
        }
        if (!VisionModClient.enabled || (!VisionExtras.hearts && !VisionExtras.snow)) {
            clear();
            return;
        }

        for (int i = count - 1; i >= 0; i--) {
            Sprite p = ACTIVE[i];
            if ((p.type == HEART_KIND && !VisionExtras.hearts) ||
                (p.type == SNOW_KIND && !VisionExtras.snow) ||
                ++p.age >= p.lifetime) {
                removeAt(i);
                continue;
            }
            p.prevX = p.x;
            p.prevY = p.y;
            p.prevZ = p.z;
            // Both sprite types drift down like snow. Hearts are slower and
            // gently flutter sideways, not vanilla rising love particles.
            p.x += p.dx + Math.sin(p.phase + p.age*.11) * .003;
            p.y += p.dy;
            p.z += p.dz + Math.cos(p.phase + p.age*.09) * .003;
        }

        if (mc.screen != null) return;
        int density = Math.max(1, Math.min(3, VisionExtras.ambientDensity));
        int interval = Math.max(4, VisionPerformance.ambientInterval() * 2 / density);
        if (++spawnTicks < interval) return;
        spawnTicks = 0;

        var camera = mc.gameRenderer.getMainCamera();
        Vec3 eye = camera.isInitialized() ? camera.position() : mc.player.getEyePosition();
        Vec3 forward = camera.isInitialized()
            ? Vec3.directionFromRotation(camera.xRot(), camera.yRot())
            : mc.player.getLookAngle();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        right = right.lengthSqr() < 0.001 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(forward).normalize();

        boolean aroundPlayer = VisionExtras.ambientStyle == 0 &&
            mc.options.getCameraType() != CameraType.FIRST_PERSON;
        Vec3 origin = aroundPlayer ? mc.player.position().add(0, 1.25, 0)
            : eye.add(forward.scale(2.8));

        for (int i = 0; i < density && count < CAPACITY; i++) {
            double x, y, z;
            if (aroundPlayer) {
                double angle = RANDOM.nextDouble() * Math.PI * 2.0;
                double r = 1.05 + RANDOM.nextDouble() * 2.7;
                x = origin.x + Math.cos(angle) * r;
                y = origin.y - .55 + RANDOM.nextDouble() * 2.5;
                z = origin.z + Math.sin(angle) * r;
            } else {
                double angle = RANDOM.nextDouble() * Math.PI * 2.0;
                double r = .4 + RANDOM.nextDouble() * 1.7;
                Vec3 point = origin
                    .add(right.scale(Math.cos(angle) * r))
                    .add(up.scale(Math.sin(angle) * r))
                    .add(forward.scale((RANDOM.nextDouble()-.5)*1.1));
                x = point.x;
                y = point.y;
                z = point.z;
            }
            if (VisionExtras.hearts) spawn(HEART_KIND, x, y, z);
            if (VisionExtras.snow) spawn(SNOW_KIND,
                x + (RANDOM.nextDouble()-.5)*.7,
                y + RANDOM.nextDouble()*.4,
                z + (RANDOM.nextDouble()-.5)*.7);
        }
    }

    private static void spawn(int kind, double x, double y, double z) {
        if (count >= CAPACITY) return;
        Sprite p = new Sprite();
        p.type = kind;
        p.x = p.prevX = x;
        p.y = p.prevY = y;
        p.z = p.prevZ = z;
        p.lifetime = 64 + RANDOM.nextInt(43);
        p.phase = RANDOM.nextDouble() * Math.PI * 2;
        p.dx = (RANDOM.nextDouble()-.5)*.012;
        p.dy = kind == HEART_KIND ? -.008 - RANDOM.nextDouble()*.008 :
            -.013 - RANDOM.nextDouble()*.011;
        p.dz = (RANDOM.nextDouble()-.5)*.012;
        p.size = kind == HEART_KIND ? .085f + RANDOM.nextFloat()*.045f :
            .075f + RANDOM.nextFloat()*.040f;
        p.spin = (RANDOM.nextFloat()-.5f) * .8f;
        p.color = kind == HEART_KIND
            ? (RANDOM.nextBoolean() ? 0xFFF5C4EF : 0xFFFFD7E6)
            : 0xFFF2F8FF;
        ACTIVE[count++] = p;
    }

    /**
     * Called only by the optional CI startup test, before a world is loaded.
     * Covers both sprite kinds, the memory cap, and immediate type cleanup.
     */
    static void selfTestPool() {
        if (count != 0) throw new IllegalStateException("Ambient pool not empty at startup");
        for (int i = 0; i < CAPACITY + 20; i++)
            spawn(i % 2 == 0 ? HEART_KIND : SNOW_KIND, 0, 1, 0);
        if (count != CAPACITY) throw new IllegalStateException("Ambient cap exceeded");
        clearType(HEART_KIND);
        if (count != CAPACITY / 2) throw new IllegalStateException("Heart cleanup failed");
        for (int i = 0; i < count; i++)
            if (ACTIVE[i].type != SNOW_KIND)
                throw new IllegalStateException("Sprite kinds got mixed");
        clear();
        if (count != 0) throw new IllegalStateException("Ambient cleanup failed");
    }

    /** Pure resource/limit checks used by the CI-only client startup test. */
    public static int capacity() { return CAPACITY; }
    public static String heartResource() { return "/assets/visionmod/textures/particle/soft_heart.png"; }
    public static String snowResource() { return "/assets/visionmod/textures/particle/soft_snow.png"; }
    public static int activeCount() { return count; }
}
