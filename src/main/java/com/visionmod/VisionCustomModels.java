package com.visionmod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.io.Reader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Small original filled JSON-driven client model attached to the local player's body.
 * No executable scripts, downloads, external model format or hidden cosmetics service.
 * Limited to 32 solid colored geometric boxes to preserve FPS; no network effects.
 */
public final class VisionCustomModels {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
        .resolve("visionmod/custom-model.json");
    private static final int MAX_BOXES = 32;
    private record Box(AABB geometry, int color) { }
    private static List<Box> boxes = List.of();
    public static boolean enabled;

    private VisionCustomModels() {}

    public static void init() {
        load();
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (!VisionModClient.enabled || !enabled || boxes.isEmpty() ||
                context.consumers() == null || context.matrices() == null) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null ||
                mc.options.getCameraType() == CameraType.FIRST_PERSON) return;

            Vec3 camera = mc.gameRenderer.getMainCamera().position();
            Vec3 player = mc.player.position();
            var pose = context.matrices();
            pose.pushPose();
            // Local geometry rotates with the player's body, NOT the Free Look camera.
            pose.translate(player.x - camera.x, player.y - camera.y + 1.08,
                player.z - camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(-mc.player.getYRot()));
            VertexConsumer fill = context.consumers().getBuffer(
                RenderTypes.entityTranslucent(VisionGeometry.WHITE_TEXTURE));
            for (Box b : boxes) {
                AABB a=b.geometry();
                VisionGeometry.cube(fill,pose,
                    (float)a.minX,(float)a.minY,(float)a.minZ,
                    (float)a.maxX,(float)a.maxY,(float)a.maxZ,b.color());
            }
            pose.popPose();
        });
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (enabled) load();
    }

    public static void load() {
        try {
            Files.createDirectories(FILE.getParent());
            if (!Files.isRegularFile(FILE)) {
                String sample = """
                    {
                      "name": "Small Backpack",
                      "boxes": [
                        {"x":-0.23,"y":-0.25,"z":0.16,"w":0.46,"h":0.48,"d":0.20,"color":"A89CFF"},
                        {"x":-0.17,"y":-0.17,"z":0.37,"w":0.34,"h":0.22,"d":0.05,"color":"F38CC9"}
                      ]
                    }
                    """;
                Files.writeString(FILE, sample, StandardCharsets.UTF_8);
            }
            List<Box> parsed = new ArrayList<>();
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                JsonObject doc = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray array = doc.getAsJsonArray("boxes");
                if (array == null) throw new IOException("Missing boxes array");
                for (JsonElement el : array) {
                    if (parsed.size() >= MAX_BOXES) break;
                    JsonObject b = el.getAsJsonObject();
                    double x = number(b,"x"), y = number(b,"y"), z = number(b,"z");
                    double w = number(b,"w"), h = number(b,"h"), d = number(b,"d");
                    // Reject malformed geometry and enormous models. Keep all coordinates
                    // in player-local blocks; only simple AABBs are supported.
                    if (!valid(x,2) || !valid(y,2) || !valid(z,2) ||
                        !valid(w,1) || !valid(h,1) || !valid(d,1) ||
                        w <= 0 || h <= 0 || d <= 0) continue;
                    int color = 0xFFA89CFF;
                    if (b.has("color")) {
                        String hex = b.get("color").getAsString().replace("#","");
                        if (hex.matches("[A-Fa-f0-9]{6}"))
                            color = 0xFF000000 | Integer.parseInt(hex,16);
                    }
                    parsed.add(new Box(new AABB(x,y,z,x+w,y+h,z+d),color));
                }
            }
            boxes = List.copyOf(parsed);
        } catch (Exception e) {
            System.err.println("[VisionMod] Invalid custom-model.json: "+e);
            boxes = List.of();
        }
    }
    private static double number(JsonObject b, String key) {
        return b.has(key) ? b.get(key).getAsDouble() : 0.0;
    }
    private static boolean valid(double value, double bound) {
        return Double.isFinite(value) && Math.abs(value) <= bound;
    }
    public static String fileHint() {
        return FILE.toString();
    }
}
