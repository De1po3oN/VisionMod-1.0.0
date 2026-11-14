package com.visionmod;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Independent optional client-only features. No code/assets from the
 * proprietary reference JAR. All toggles default OFF.
 *
 * Some requested features are large: this class implements the concrete
 * lightweight subsets documented in FEATURE_REQUESTS_2_0_0.md, never claims
 * a full clone or server-side kill attribution.
 */
public final class VisionExtendedFeatures {
    public static final String[] NAMES = {
        "Block Overlay", "Kill Effect", "World Color", "World Lyrics",
        "Local Pet", "Social Radar", "Custom Sound", "Clear Weather FX"
    };
    private static final boolean[] FLAGS = new boolean[NAMES.length];
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
        .resolve("visionmod/extended-2.properties");
    private static final VoxelShape PET_BODY = Shapes.create(new AABB(-.14, -.12, -.11, .14, .12, .11));
    private static final VoxelShape PET_EAR_LEFT = Shapes.create(new AABB(-.12, .08, -.09, -.05, .21, -.02));
    private static final VoxelShape PET_EAR_RIGHT = Shapes.create(new AABB(.05, .08, -.09, .12, .21, -.02));
    private static final VoxelShape PET_TAIL = Shapes.create(new AABB(.10, -.07, .07, .22, -.02, .23));
    private static final VoxelShape OVERLAY_INNER = Shapes.create(new AABB(
        .17, .17, .17, .83, .83, .83));

    private static int ticks;
    private static int notificationTicks;
    private static int previousVictimId = -1;
    private static boolean wasHurting;
    private static String lastTarget = "";
    private static String lyric = "";
    private static String friends = "";
    private static String friendsNearby = "";
    private static int worldColor = 0;
    private static int petColor = 0xFFA89CFF;

    private VisionExtendedFeatures() {}

    public static int count() { return NAMES.length; }
    public static boolean enabled(int i) { return i >= 0 && i < FLAGS.length && FLAGS[i]; }
    public static boolean clearWeather() { return VisionModClient.enabled && FLAGS[7]; }
    public static void toggle(int i) {
        if (i < 0 || i >= FLAGS.length) return;
        FLAGS[i] = !FLAGS[i];
        save();
    }
    public static void cycleWorldColor() {
        worldColor = (worldColor + 1) % 3;
        save();
    }
    public static String colorLabel() {
        return switch (worldColor) { case 1 -> "WARM"; case 2 -> "COOL"; default -> "DUSK"; };
    }

    public static void init() {
        load();
        ClientTickEvents.END_CLIENT_TICK.register(VisionExtendedFeatures::tick);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("visionmod", "extended_hud"),
            (g, tracker) -> renderHud(g));
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (!VisionModClient.enabled || context.matrices() == null ||
                context.consumers() == null || (!FLAGS[0] && !FLAGS[4])) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            Vec3 camera = mc.gameRenderer.getMainCamera().position();
            VertexConsumer lines = context.consumers().getBuffer(RenderTypes.lines());

            if (FLAGS[0] && mc.hitResult instanceof BlockHitResult hit &&
                hit.getType() == HitResult.Type.BLOCK && mc.level.hasChunkAt(hit.getBlockPos())) {
                BlockState state = mc.level.getBlockState(hit.getBlockPos());
                if (!state.isAir()) {
                    var bp = hit.getBlockPos();
                    ShapeRenderer.renderShape(context.matrices(), lines, OVERLAY_INNER,
                        bp.getX() - camera.x, bp.getY() - camera.y, bp.getZ() - camera.z,
                        0xFFB5A0FF, 1.5F);
                }
            }
            if (FLAGS[4]) {
                // Tiny original floating cat silhouette, only local to this client.
                double t = mc.level.getGameTime() / 8.0;
                Vec3 p = mc.player.position().add(
                    Math.cos(t * .35) * .85, 1.25 + Math.sin(t) * .15,
                    Math.sin(t * .35) * .85);
                double x = p.x - camera.x, y = p.y - camera.y, z = p.z - camera.z;
                for (VoxelShape shape : new VoxelShape[]{PET_BODY, PET_EAR_LEFT, PET_EAR_RIGHT, PET_TAIL}) {
                    ShapeRenderer.renderShape(context.matrices(), lines, shape, x, y, z, petColor, 1.5F);
                }
            }
        });
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            ticks = 0;
            notificationTicks = 0;
            previousVictimId = -1;
            lastTarget = "";
            friendsNearby = "";
            wasHurting = false;
            return;
        }
        if (!VisionModClient.enabled) return;
        if (notificationTicks > 0) notificationTicks--;

        // Hurt-time feedback, not a forged kill confirmation. Client never
        // claims server-authoritative attribution.
        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target
            && target != mc.player) {
            boolean hurting = target.hurtTime > 0;
            int entityId = target.getId();
            if (FLAGS[6] && hurting && (previousVictimId != entityId || !wasHurting)) {
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, .30F, 1.35F);
            }
            if (FLAGS[1] && !target.isAlive() && previousVictimId == entityId) {
                notificationTicks = 24;
                lastTarget = target.getName().getString();
            }
            previousVictimId = entityId;
            wasHurting = hurting;
        } else {
            previousVictimId = -1;
            wasHurting = false;
        }

        if (++ticks >= 20) {
            ticks = 0;
            if (FLAGS[5] && !friends.isBlank()) {
                StringBuilder near = new StringBuilder();
                for (Player other : mc.level.players()) {
                    if (other == mc.player || other.distanceToSqr(mc.player) > 48 * 48) continue;
                    String name = other.getGameProfile().name();
                    for (String friend : friends.split(",")) {
                        if (!friend.trim().isEmpty() && friend.trim().equalsIgnoreCase(name)) {
                            if (near.length() > 0) near.append(", ");
                            if (near.length() < 95) near.append(name);
                            break;
                        }
                    }
                }
                friendsNearby = near.toString();
            } else {
                friendsNearby = "";
            }
        }
    }

    private static void renderHud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!VisionModClient.enabled || mc.player == null || mc.level == null || mc.screen != null) return;
        int w = g.guiWidth(), h = g.guiHeight();

        if (FLAGS[2]) {
            int tint = switch(worldColor) {
                case 1 -> 0x14FFC884;
                case 2 -> 0x1B5C91FF;
                default -> 0x138C5DBB;
            };
            g.fill(0, 0, w, h, tint);
        }
        if (FLAGS[3] && !lyric.isBlank()) {
            String current = lyric;
            while (current.length() > 0 && mc.font.width(current) > w - 36)
                current = current.substring(0, current.length() - 1);
            int x = (w - mc.font.width(current)) / 2;
            g.fill(x - 10, h - 79, x + mc.font.width(current) + 10, h - 57, 0xA0141326);
            g.drawString(mc.font, current, x, h - 72, 0xFFE6DFFF);
        }
        if (FLAGS[5]) {
            String label = friendsNearby.isEmpty() ? "Social Radar: no friends in range" : "Friends: " + friendsNearby;
            g.drawString(mc.font, label, 12, h - 50, 0xFFB6A8FF);
        }
        if (FLAGS[1] && notificationTicks > 0) {
            int a = Math.min(255, notificationTicks * 10);
            String title = "DEFEATED: " + lastTarget;
            int x = (w - mc.font.width(title)) / 2;
            g.drawString(mc.font, title, x, h / 2 - 38, (a << 24) | 0x00E7B5FF);
        }
    }

    private static void load() {
        if (!Files.isRegularFile(FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
            for (int i = 0; i < FLAGS.length; i++)
                FLAGS[i] = Boolean.parseBoolean(p.getProperty("feature." + i, "false"));
            lyric = p.getProperty("ownLyric", "").replace("\r", " ").replace("\n", " ").trim();
            friends = p.getProperty("friends", "");
            worldColor = Math.max(0, Math.min(2, Integer.parseInt(p.getProperty("worldColor", "0"))));
            petColor = (int)Long.parseLong(p.getProperty("petColor", "ffa89cff"),16);
        } catch(Exception e) {
            System.err.println("[VisionMod] Extended feature settings read failed: " + e);
        }
    }
    private static void save() {
        Properties p = new Properties();
        for (int i = 0; i < FLAGS.length; i++)
            p.setProperty("feature." + i, Boolean.toString(FLAGS[i]));
        p.setProperty("ownLyric", lyric);
        p.setProperty("friends", friends);
        p.setProperty("worldColor", Integer.toString(worldColor));
        p.setProperty("petColor", Integer.toHexString(petColor));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "VisionMod 2.0 original optional world extras; ownLyric and friends are local text");
            }
        } catch (Exception e) {
            System.err.println("[VisionMod] Extended feature settings save failed: " + e);
        }
    }
}
