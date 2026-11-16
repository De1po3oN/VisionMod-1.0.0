package com.visionmod;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
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
    private static final VoxelShape BLOCK_OUTLINE = Shapes.create(new AABB(
        .006, .006, .006, .994, .994, .994));

    private static int ticks;
    private static int notificationTicks;
    private static int lastPlayedHitSequence;
    private static int lastShownDeathSequence;
    private static String lastTarget = "";
    private static String lyric = "";
    private static String friends = "";
    private static String friendsNearby = "";
    private static java.util.List<String> friendRows=java.util.List.of();
    private static int soundPreset;
    public static String soundLabel(){return switch(soundPreset){case 1->"AMETHYST";case 2->"CHIME";default->"SOFT";};}
    public static int soundPreset(){return soundPreset;}
    public static void cycleSound(){soundPreset=(soundPreset+1)%3;save();}
    private static int worldColor = 0;
    private static int petColor = 0xFFE6CAFC;
    private static final int[] PET_PALETTE={
        0xFFE6CAFC,0xFFFFB9CF,0xFFB3F5E4,0xFFFFD18E,0xFFAACFFF,0xFFFFFFFF
    };
    private static final String[] PET_COLORS={
        "LAVENDER","PINK","MINT","HONEY","SKY","PEARL"
    };

    private VisionExtendedFeatures() {}

    public static int count() { return NAMES.length; }
    public static boolean enabled(int i) { return i >= 0 && i < FLAGS.length && FLAGS[i]; }
    public static boolean clearWeather() { return VisionModClient.enabled && FLAGS[7]; }
    public static void toggle(int i) {
        if (i < 0 || i >= FLAGS.length) return;
        FLAGS[i] = !FLAGS[i];
        if (i==4) VisionPetRenderer.retryAfterToggle();
        // Earlier builds persisted an empty ownLyric, making World Lyrics
        // appear totally broken even when its button was enabled.
        save();
    }
    public static void cycleWorldColor() {
        worldColor = (worldColor + 1) % 3;
        save();
    }
    public static String colorLabel() {
        return switch (worldColor) { case 1 -> "WARM"; case 2 -> "COOL"; default -> "DUSK"; };
    }
    public static int worldColor(){return worldColor;}
    public static void setWorldColor(int value){worldColor=Math.clamp(value,0,2);save();}
    public static void setPetColor(int value){petColor=value|0xFF000000;save();}
    public static int petColor() { return petColor; }
    public static String petColorLabel() {
        for(int i=0;i<PET_PALETTE.length;i++)
            if(PET_PALETTE[i]==petColor)return PET_COLORS[i];
        return "CUSTOM";
    }
    public static void cyclePetColor() {
        int next=0;
        for(int i=0;i<PET_PALETTE.length;i++) {
            if(PET_PALETTE[i]==petColor){next=(i+1)%PET_PALETTE.length;break;}
        }
        petColor=PET_PALETTE[next];
        save();
    }

    public static String customText() { return lyric; }
    public static String friendNames() { return friends; }
    public static String nearbyFriends() { return friendsNearby.isBlank() ? "No friends nearby" : friendsNearby; }

    /** Edit user-owned text directly in the GUI; never ship copied lyrics. */
    public static void setCustomText(String text) {
        lyric = text == null ? "" : text.replaceAll("[\\r\\n\\p{Cntrl}]", " ").trim();
        if (lyric.length() > 120) lyric = lyric.substring(0, 120);
        save();
    }
    public static void setFriendNames(String text) {
        friends = text == null ? "" : text.replaceAll("[\\r\\n\\p{Cntrl}]", " ").trim();
        if (friends.length() > 120) friends = friends.substring(0, 120);
        save();
    }

    public static void init() {
        load();
        ClientTickEvents.END_CLIENT_TICK.register(VisionExtendedFeatures::tick);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("visionmod", "extended_hud"),
            (g, tracker) -> renderHud(g));
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (!VisionModClient.enabled || context.matrices() == null ||
                context.consumers() == null || !FLAGS[0]) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            Vec3 camera=mc.gameRenderer.getMainCamera().position();
            if(mc.hitResult instanceof BlockHitResult hit&&hit.getType()==HitResult.Type.BLOCK&&mc.level.hasChunkAt(hit.getBlockPos())){
                var bp=hit.getBlockPos();BlockState state=mc.level.getBlockState(bp);
                if(state.isAir())return;
                VoxelShape shape=state.getShape(mc.level,bp);
                if(shape.isEmpty())return;
                double x=bp.getX()-camera.x,y=bp.getY()-camera.y,z=bp.getZ()-camera.z;
                VertexConsumer fill=context.consumers().getBuffer(RenderTypes.entityTranslucent(VisionGeometry.WHITE_TEXTURE));
                int alpha=VisionAppearance.animations?27+(int)(5*Math.sin(mc.level.getGameTime()*.09)):30;
                int count=0;
                for(AABB box:shape.toAabbs()){
                    if(count++>=64)break;
                    VisionGeometry.cube(fill,context.matrices(),(float)(x+box.minX-.002),(float)(y+box.minY-.002),(float)(z+box.minZ-.002),
                        (float)(x+box.maxX+.002),(float)(y+box.maxY+.002),(float)(z+box.maxZ+.002),VisionGeometry.withAlpha(VisionMenuTheme.PURPLE,alpha));
                }
                VertexConsumer lines=context.consumers().getBuffer(RenderTypes.lines());
                ShapeRenderer.renderShape(context.matrices(),lines,shape,x,y,z,VisionGeometry.withAlpha(VisionMenuTheme.CYAN,180),1.5f);
            }

        });
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            ticks = 0;
            notificationTicks = 0;
            lastPlayedHitSequence = VisionCombatTracker.hitSequence();
            lastShownDeathSequence = VisionCombatTracker.deathSequence();
            lastTarget = "";
            friendsNearby = "";friendRows=java.util.List.of();
            return;
        }
        if (!VisionModClient.enabled) {
            lastPlayedHitSequence = VisionCombatTracker.hitSequence();
            lastShownDeathSequence = VisionCombatTracker.deathSequence();
            return;
        }
        if (notificationTicks > 0) notificationTicks--;

        int hitEvent = VisionCombatTracker.hitSequence();
        if (hitEvent != lastPlayedHitSequence) {
            lastPlayedHitSequence = hitEvent;
            if (FLAGS[6]) {
                if(soundPreset==1)mc.player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME,.28F,1.2F);
                else if(soundPreset==2)mc.player.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(),.25F,1.5F);
                else mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP,.20F,1.55F);
            }
        }
        int deathEvent = VisionCombatTracker.deathSequence();
        if (deathEvent != lastShownDeathSequence) {
            lastShownDeathSequence = deathEvent;
            if (FLAGS[1]) {
                notificationTicks = 28;
                lastTarget = VisionCombatTracker.deathName();
            }
        }

        if (++ticks >= 20) {
            ticks = 0;
            if (FLAGS[5] && !friends.isBlank()) {
                StringBuilder near = new StringBuilder();
                java.util.ArrayList<String> rows=new java.util.ArrayList<>();
                for (Player other : mc.level.players()) {
                    if (other == mc.player || other.distanceToSqr(mc.player) > 48 * 48) continue;
                    String name = other.getGameProfile().name();
                    for (String friend : friends.split(",")) {
                        if (!friend.trim().isEmpty() && friend.trim().equalsIgnoreCase(name)) {
                            if (near.length() > 0) near.append(", ");
                            if (near.length() < 95) near.append(name);
                            if(rows.size()<6)rows.add(name+"  ·  "+Math.round(Math.sqrt(other.distanceToSqr(mc.player)))+" m");
                            break;
                        }
                    }
                }
                friendsNearby = near.toString();friendRows=java.util.List.copyOf(rows);
            } else {
                friendsNearby = "";friendRows=java.util.List.of();
            }
        }
    }

    private static void renderHud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!VisionModClient.enabled || mc.player == null || mc.level == null ||
            !VisionHudLayout.hudVisible(mc)) return;
        int w = g.guiWidth(), h = g.guiHeight();

        if (FLAGS[2]) {
            // Soft two-stop atmospheric treatment instead of a uniform flat wash.
            // Kept intentionally low opacity and free of GPU post-processing.
            int top = switch(worldColor) {
                case 1 -> 0x25FFC48A; // sunset
                case 2 -> 0x226F9AFF; // cold moonlight
                default -> 0x238F70DA; // violet dusk
            };
            int bottom = switch(worldColor) {
                case 1 -> 0x22F28D73;
                case 2 -> 0x1E4577D0;
                default -> 0x1B364E9B;
            };
            g.fillGradient(0,0,w,h,top,bottom);
        }
        if(FLAGS[1]&&notificationTicks>0){
            float progress=1-notificationTicks/28f;
            int alpha=(int)(255*Math.min(1,notificationTicks/8f));
            int width=Math.min(w-24,Math.max(166,mc.font.width(lastTarget)+32));
            int lift=VisionAppearance.animations?(int)(5*(1-EffectMath.ease(progress))):0;
            var box=VisionHudLayout.place(VisionHudLayout.Widget.KILL,(w-width)/2,h/2-65-lift,width,42);
            VisionHudTheme.panel(g,box.x(),box.y(),box.width(),box.height(),VisionMenuTheme.PURPLE);
            g.drawString(mc.font,"TARGET DOWN",box.x()+12,box.y()+8,VisionGeometry.withAlpha(VisionMenuTheme.PURPLE,alpha));
            VisionHudTheme.text(g,mc.font,lastTarget,box.x()+12,box.y()+24,box.width()-24,VisionGeometry.withAlpha(VisionMenuTheme.WHITE,alpha));
        }
        if(FLAGS[5]){
            int height=30+Math.max(1,friendRows.size())*14;
            var box=VisionHudLayout.place(VisionHudLayout.Widget.SOCIAL,w-168,h-height-18,156,height);
            VisionHudTheme.panel(g,box.x(),box.y(),box.width(),box.height(),0xFF96E2C2);
            g.drawString(mc.font,"ДРУЗЬЯ РЯДОМ",box.x()+10,box.y()+9,0xFF96E2C2);
            if(friendRows.isEmpty())g.drawString(mc.font,"Никого в радиусе 48 m",box.x()+10,box.y()+27,VisionMenuTheme.MUTED);
            for(int i=0;i<friendRows.size();i++)VisionHudTheme.text(g,mc.font,friendRows.get(i),box.x()+10,box.y()+27+i*14,box.width()-20,VisionMenuTheme.WHITE);
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
            if (lyric.equals("YOUR WORLD | YOUR STORY")) lyric="";
            friends = p.getProperty("friends", "");
            worldColor = Math.max(0, Math.min(2, Integer.parseInt(p.getProperty("worldColor", "0"))));
            soundPreset=Math.clamp(Integer.parseInt(p.getProperty("soundPreset","0")),0,2);
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
        p.setProperty("soundPreset",Integer.toString(soundPreset));
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
