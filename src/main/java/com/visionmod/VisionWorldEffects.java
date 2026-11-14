package com.visionmod;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;

/**
 * Independent low-complexity world effects.
 * Reuses the same render event and line pipeline as TargetOutline. No shaders,
 * texture downloads, server hooks, or persistent tracking of other players.
 */
public final class VisionWorldEffects {
    public static final String[] NAMES = {
        "Target ESP", "Trails", "Jump Circles", "China Hat"
    };
    private static final boolean[] ENABLED = new boolean[NAMES.length];
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir()
        .resolve("visionmod/world-effects-2.properties");
    private static final VoxelShape TRAIL_DOT = Shapes.create(
        new AABB(-.045, .01, -.045, .045, .03, .045));
    private static final VoxelShape[] RING_DOTS = makeCircle(24, 1.0, .06, .012);
    private static final VoxelShape[] HAT_DOTS = makeHat();
    private static final Deque<Vec3> TRAIL = new ArrayDeque<>();
    private static ClientLevel previousWorld;
    private static Vec3 jumpPos;
    private static int jumpAge;
    private static boolean grounded;
    private static int ticks;

    private VisionWorldEffects() { }

    public static int count() { return NAMES.length; }
    public static boolean enabled(int i) {
        return i >= 0 && i < ENABLED.length && ENABLED[i];
    }
    public static void toggle(int i) {
        if (i < 0 || i >= ENABLED.length) return;
        ENABLED[i] = !ENABLED[i];
        if (i == 1 && !ENABLED[i]) TRAIL.clear();
        save();
    }
    public static void init() {
        load();
        ClientTickEvents.END_CLIENT_TICK.register(VisionWorldEffects::tick);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (!VisionModClient.enabled || context.consumers() == null ||
                context.matrices() == null) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            boolean any = false;
            for (boolean b : ENABLED) any |= b;
            if (!any) return;

            Vec3 camera = mc.gameRenderer.getMainCamera().position();
            VertexConsumer lines = context.consumers().getBuffer(RenderTypes.lines());
            int color = VisionMenuTheme.CYAN;
            if (ENABLED[0] && mc.hitResult instanceof EntityHitResult hit &&
                hit.getEntity() instanceof LivingEntity target && target != mc.player) {
                AABB box = target.getBoundingBox().inflate(.035);
                ShapeRenderer.renderShape(context.matrices(), lines,
                    Shapes.create(box), -camera.x, -camera.y, -camera.z,
                    VisionMenuTheme.PURPLE, 1.7F);
            }

            if (ENABLED[1]) {
                for (Vec3 point : TRAIL) {
                    ShapeRenderer.renderShape(context.matrices(), lines, TRAIL_DOT,
                        point.x - camera.x, point.y - camera.y + .08,
                        point.z - camera.z, color, 1.2F);
                }
            }
            if (ENABLED[2] && jumpAge > 0 && jumpPos != null) {
                int alpha = 40 + jumpAge * 7;
                int fadingColor = (Math.min(255, alpha) << 24) | (color & 0xFFFFFF);
                for (VoxelShape dot : RING_DOTS) {
                    ShapeRenderer.renderShape(context.matrices(), lines, dot,
                        jumpPos.x - camera.x, jumpPos.y - camera.y + .035,
                        jumpPos.z - camera.z, fadingColor, 1.3F);
                }
            }
            // Local, third-person-only decorative hat, not the old cosmetics service.
            if (ENABLED[3] && mc.options.getCameraType() != CameraType.FIRST_PERSON) {
                Vec3 p = mc.player.position();
                double top = p.y + mc.player.getBbHeight() + .07;
                for (VoxelShape dot : HAT_DOTS) {
                    ShapeRenderer.renderShape(context.matrices(), lines, dot,
                        p.x - camera.x, top - camera.y, p.z - camera.z,
                        VisionMenuTheme.PURPLE, 1.25F);
                }
            }
        });
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            reset(null);
            return;
        }
        if (previousWorld != mc.level) reset(mc.level);
        if (!VisionModClient.enabled) return;

        if (ENABLED[1] && ++ticks >= 4) {
            ticks = 0;
            Vec3 p = mc.player.position();
            if (TRAIL.isEmpty() || TRAIL.peekLast().distanceToSqr(p) > .13) {
                TRAIL.addLast(p);
                while (TRAIL.size() > 13) TRAIL.removeFirst();
            }
        }
        if (ENABLED[2]) {
            boolean nowGround = mc.player.onGround();
            if (nowGround && !grounded && !mc.player.isPassenger()) {
                jumpPos = mc.player.position();
                jumpAge = 20;
            }
            grounded = nowGround;
            if (jumpAge > 0) jumpAge--;
        } else {
            grounded = mc.player.onGround();
            jumpAge = 0;
        }
    }

    private static void reset(ClientLevel level) {
        previousWorld = level;
        TRAIL.clear();
        jumpPos = null;
        jumpAge = ticks = 0;
        grounded = false;
    }

    private static VoxelShape[] makeCircle(int samples, double radius, double width, double height) {
        VoxelShape[] result = new VoxelShape[samples];
        for (int i = 0; i < samples; i++) {
            double theta = i * Math.PI * 2.0 / samples;
            double x = Math.cos(theta) * radius, z = Math.sin(theta) * radius;
            result[i] = Shapes.create(new AABB(x - width / 2, 0, z - width / 2,
                x + width / 2, height, z + width / 2));
        }
        return result;
    }

    private static VoxelShape[] makeHat() {
        // Two concentric rings plus tapered intermediate dots: low-poly cone.
        int steps = 18;
        VoxelShape[] result = new VoxelShape[steps * 3];
        for (int i = 0; i < steps; i++) {
            double angle = i * 2.0 * Math.PI / steps;
            double cs = Math.cos(angle), sn = Math.sin(angle);
            for (int level = 0; level < 3; level++) {
                double radius = .53 - level * .19;
                double height = .015 + level * .15;
                double x = cs * radius, z = sn * radius;
                result[i * 3 + level] = Shapes.create(new AABB(
                    x - .026, height, z - .026,
                    x + .026, height + .012, z + .026));
            }
        }
        return result;
    }

    private static void load() {
        if (!Files.isRegularFile(CONFIG)) return;
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG)) {
            properties.load(in);
            for (int i = 0; i < ENABLED.length; i++)
                ENABLED[i] = Boolean.parseBoolean(properties.getProperty("effect." + i, "false"));
        } catch (Exception ex) {
            System.err.println("[VisionMod] World Effects config read failed: " + ex);
        }
    }
    private static void save() {
        Properties properties = new Properties();
        for (int i = 0; i < ENABLED.length; i++)
            properties.setProperty("effect." + i, Boolean.toString(ENABLED[i]));
        try {
            Files.createDirectories(CONFIG.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG)) {
                properties.store(out, "Vision Mod 2.0: bounded world render effects");
            }
        } catch (Exception ex) {
            System.err.println("[VisionMod] World Effects config save failed: " + ex);
        }
    }
}
