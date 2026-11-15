package com.visionmod;

import com.mojang.blaze3d.vertex.PoseStack;
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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;

/**
 * Bounded client-only world visuals. No copied proprietary assets or shaders.
 *
 * Continuous gradient ribbons, expanding radial ground circles, a translucent
 * cone hat, and pulsing target outline replace the former sparse 3D boxes.
 * All effects can be disabled without retaining GPU resources or network data.
 */
public final class VisionWorldEffects {
    public static final String[] NAMES = {
        "Target ESP", "Trails", "Jump Circles", "China Hat"
    };
    private static final boolean[] ENABLED = new boolean[NAMES.length];
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir()
        .resolve("visionmod/world-effects-2.properties");
    private static final int SEGMENTS=36;
    private static final int MAX_TRAIL=25;
    private static final long TRAIL_LIFETIME=28;
    // Preserved in world-effects-2.properties, independent of menu/GUI state.
    private static int trailStartColor = 0xFF67E8F9;
    private static int trailEndColor = 0xFFB66DFF;
    /** 0 solid, 1 two-color gradient, 2 smooth cycling rainbow. */
    private static int trailMode = 1;
    private static float trailWidth = 0.115f;
    private static int trailOpacity = 160;

    public static int trailStartColor() { return trailStartColor; }
    public static int trailEndColor() { return trailEndColor; }
    public static int trailMode() { return trailMode; }
    public static String trailModeLabel() {
        return switch(trailMode) { case 0 -> "SOLID"; case 2 -> "RAINBOW"; default -> "GRADIENT"; };
    }
    public static float trailWidth() { return trailWidth; }
    public static int trailOpacity() { return trailOpacity; }
    public static String trailColorHex(int value) {
        return String.format(java.util.Locale.ROOT, "#%06X", value & 0xFFFFFF);
    }
    /** Strictly accept #RRGGBB, RRGGBB, or decimal R,G,B. */
    public static int parseTrailColor(String value) {
        if (value == null) throw new IllegalArgumentException("No color");
        String input = value.trim();
        if (input.matches("(?i)#?[0-9a-f]{6}"))
            return 0xFF000000 | Integer.parseInt(input.replace("#", ""), 16);
        String[] channels = input.split(",",-1);
        if (channels.length != 3) throw new IllegalArgumentException("Use #RRGGBB or R,G,B");
        int color = 0xFF000000;
        for (int i=0;i<3;i++) {
            int v;
            try { v = Integer.parseInt(channels[i].trim()); }
            catch(NumberFormatException e) { throw new IllegalArgumentException("RGB channels must be numbers"); }
            if (v < 0 || v > 255) throw new IllegalArgumentException("RGB channels must be 0-255");
            color |= v << (16 - 8*i);
        }
        return color;
    }
    public static void setTrailColor(int index,int color) {
        if (index==0) trailStartColor = 0xFF000000 | (color & 0xFFFFFF);
        else if (index==1) trailEndColor = 0xFF000000 | (color & 0xFFFFFF);
        else return;
        save();
    }
    public static void setTrailMode(int mode) {
        trailMode = Math.max(0,Math.min(2,mode)); save();
    }
    public static void cycleTrailMode() { setTrailMode((trailMode+1)%3); }
    public static void cycleTrailWidth() {
        trailWidth = trailWidth < .09f ? .115f : trailWidth < .15f ? .22f : .065f;
        save();
    }
    public static void cycleTrailOpacity() {
        trailOpacity = trailOpacity <= 110 ? 160 : trailOpacity <= 160 ? 220 : 90;
        save();
    }
    public static int trailSampleColor(float along,long ticks) {
        float t=Math.max(0f,Math.min(1f,along));
        return switch(trailMode) {
            case 0 -> trailStartColor;
            case 2 -> 0xFF000000 | (java.awt.Color.HSBtoRGB(
                (float)((t*.85 + ticks*.009) % 1.0),.83f,1f) & 0xFFFFFF);
            default -> VisionGeometry.blendRgb(trailStartColor,trailEndColor,t);
        };
    }
    private record TrailPoint(Vec3 position,long time) {}
    private static final Deque<TrailPoint> TRAIL = new ArrayDeque<>(MAX_TRAIL);
    private static ClientLevel previousWorld;
    private static Vec3 landingPos;
    private static int landingAge;
    private static boolean grounded;
    private static int tick;
    private static boolean renderFaulted;

    private VisionWorldEffects() {}
    public static int count() { return NAMES.length; }
    public static boolean enabled(int id) {
        return id>=0 && id<ENABLED.length && ENABLED[id];
    }
    public static void toggle(int id) {
        if (id<0 || id>=ENABLED.length) return;
        ENABLED[id]=!ENABLED[id];
        if (id==1 && !ENABLED[id]) TRAIL.clear();
        if (id==2 && !ENABLED[id]) landingAge=0;
        save();
    }

    public static void init() {
        load();
        ClientTickEvents.END_CLIENT_TICK.register(VisionWorldEffects::tick);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(ctx -> {
            if (renderFaulted || !VisionModClient.enabled || ctx.consumers()==null || ctx.matrices()==null)
                return;
            Minecraft mc=Minecraft.getInstance();
            if(mc.level==null || mc.player==null ||
                (!ENABLED[0] && !ENABLED[1] && !ENABLED[2] && !ENABLED[3])) return;
            Vec3 cam=mc.gameRenderer.getMainCamera().position();
            PoseStack pose=ctx.matrices();
            try {
            // IMPORTANT: a shared ImmediateBufferSource finalizes the previous
            // draw buffer whenever its render type changes. Never retain and
            // write to a line consumer after obtaining the fill consumer.
            // Render ALL translucent geometry first, ALL outlines second.
            // One shared original texture, no extra GPU resources.
            VertexConsumer fill=ctx.consumers().getBuffer(
                RenderTypes.entityTranslucent(VisionGeometry.WHITE_TEXTURE));
            double t=System.nanoTime()*1.0e-9;

            if(ENABLED[1] && TRAIL.size()>=2) {
                TrailPoint last=null;
                long worldTime=mc.level.getGameTime();
                int idx=0, samples=TRAIL.size();
                for(TrailPoint current:TRAIL) {
                    if(last!=null) {
                        Vec3 a=last.position(),b=current.position();
                        double dx=b.x-a.x,dz=b.z-a.z;
                        double length=Math.sqrt(dx*dx+dz*dz);
                        // Never join teleportations, world transitions or server corrections.
                        if(length>.025 && length<2.5 && Math.abs(b.y-a.y)<1.7) {
                            float strength=(float)Math.max(0,1.0-
                                (worldTime-current.time())/(double)TRAIL_LIFETIME);
                            float width=trailWidth*(.35f+.65f*strength);
                            double px=-dz/length*width,pz=dx/length*width;
                            int alpha=(int)(strength*trailOpacity);
                            int colorA=VisionGeometry.withAlpha(
                                trailSampleColor((idx-1f)/(samples-1f),worldTime),
                                Math.max(0,alpha-18));
                            int colorB=VisionGeometry.withAlpha(
                                trailSampleColor(idx/(float)(samples-1),worldTime),alpha);
                            float ax=(float)(a.x-cam.x),az=(float)(a.z-cam.z);
                            float bx=(float)(b.x-cam.x),bz=(float)(b.z-cam.z);
                            float ay=(float)(a.y-cam.y+.15),by=(float)(b.y-cam.y+.15);
                            VisionGeometry.vertex(fill,pose,(float)(ax+px),ay,(float)(az+pz),0,0,colorA,0,1,0);
                            VisionGeometry.vertex(fill,pose,(float)(ax-px),ay,(float)(az-pz),1,0,colorA,0,1,0);
                            VisionGeometry.vertex(fill,pose,(float)(bx-px),by,(float)(bz-pz),1,1,colorB,0,1,0);
                            VisionGeometry.vertex(fill,pose,(float)(bx+px),by,(float)(bz+pz),0,1,colorB,0,1,0);
                        }
                    }
                    last=current;
                    idx++;
                }
            }

            if(ENABLED[2] && landingAge>0 && landingPos!=null) {
                float progress=(20-landingAge)/20.0f;
                float eased=1-(1-progress)*(1-progress);
                double cy=landingPos.y-cam.y+.045;
                double cx=landingPos.x-cam.x,cz=landingPos.z-cam.z;
                int alpha=(int)(190*(1-progress));
                drawBand(fill,pose,cx,cy,cz,.22+eased*1.05,
                    .065*(1-progress)+.018,
                    VisionGeometry.withAlpha(VisionMenuTheme.CYAN,alpha),SEGMENTS);
            }

            if(ENABLED[3] && mc.options.getCameraType()!=CameraType.FIRST_PERSON) {
                // Form a genuine non-degenerate 36-sector mesh. The old
                // primitive repeated its apex as the fourth vertex and
                // produced zero-area geometry, invisible on many GPUs.
                // Each section has a small raised inner rim and wide brim.
                Vec3 p=mc.player.position();
                double cx=p.x-cam.x,cy=p.y+mc.player.getBbHeight()+.09-cam.y
                    +Math.sin(t*2.0)*.013,cz=p.z-cam.z;
                int tintA=VisionGeometry.withAlpha(VisionMenuTheme.PURPLE,185);
                int tintB=VisionGeometry.withAlpha(VisionMenuTheme.CYAN,180);
                for(int i=0;i<SEGMENTS;i++) {
                    double angle=i*2*Math.PI/SEGMENTS;
                    double next=(i+1)*2*Math.PI/SEGMENTS;
                    float x1=(float)(cx+Math.cos(angle)*.56);
                    float z1=(float)(cz+Math.sin(angle)*.56);
                    float x2=(float)(cx+Math.cos(next)*.56);
                    float z2=(float)(cz+Math.sin(next)*.56);
                    float ix1=(float)(cx+Math.cos(angle)*.012);
                    float iz1=(float)(cz+Math.sin(angle)*.012);
                    float ix2=(float)(cx+Math.cos(next)*.012);
                    float iz2=(float)(cz+Math.sin(next)*.012);
                    int color=i%2==0?tintA:tintB;
                    VisionGeometry.quad(fill,pose,
                        ix1,(float)(cy+.34),iz1,
                        x1,(float)cy,z1,
                        x2,(float)cy,z2,
                        ix2,(float)(cy+.34),iz2,
                        color,0,1,0);
                    // Also emit the reverse face for mods/shaders that cull
                    // transparent triangles. No extra render type switches.
                    VisionGeometry.quad(fill,pose,
                        ix2,(float)(cy+.34),iz2,
                        x2,(float)cy,z2,
                        x1,(float)cy,z1,
                        ix1,(float)(cy+.34),iz1,
                        color,0,-1,0);
                }
                drawBand(fill,pose,cx,cy-.008,cz,.555,.075,
                    VisionGeometry.withAlpha(VisionMenuTheme.CYAN,145),SEGMENTS);
            }

            // LAST render pass: acquire the lines buffer only once after all
            // translucent vertices have been submitted. No more fill writes
            // may follow, otherwise Minecraft throws "Not building!".
            if(ENABLED[0] || ENABLED[2] || ENABLED[3]) {
                VertexConsumer line=ctx.consumers().getBuffer(RenderTypes.lines());
                if(ENABLED[0] && mc.hitResult instanceof EntityHitResult hit &&
                    hit.getEntity() instanceof LivingEntity living &&
                    living!=mc.player && living.isAlive() &&
                    living.distanceToSqr(mc.player)<24*24) {
                    int c=VisionTools2.hitColor();
                    AABB box=living.getBoundingBox().inflate(.035);
                    ShapeRenderer.renderShape(pose,line,Shapes.create(box),
                        -cam.x,-cam.y,-cam.z,VisionGeometry.withAlpha(c,210),2f);
                    double radius=living.getBbWidth()*.64+.12;
                    drawRing(pose,line,living.getX()-cam.x,box.minY-cam.y+.035,
                        living.getZ()-cam.z,radius,VisionGeometry.withAlpha(c,160),1.5f,SEGMENTS);
                }
                if(ENABLED[2] && landingAge>0 && landingPos!=null) {
                    float progress=(20-landingAge)/20f;
                    float eased=1-(1-progress)*(1-progress);
                    double cx=landingPos.x-cam.x,cy=landingPos.y-cam.y+.045,cz=landingPos.z-cam.z;
                    int alpha=(int)(190*(1-progress));
                    drawRing(pose,line,cx,cy+.012,cz,.22+eased*1.05,
                        VisionGeometry.withAlpha(VisionMenuTheme.PURPLE,alpha),1.8f,SEGMENTS);
                    if(progress<.7f)drawRing(pose,line,cx,cy+.025,cz,
                        .12+eased*.67,VisionGeometry.withAlpha(VisionMenuTheme.CYAN,
                        (int)(80*(1-progress))),1.1f,SEGMENTS);
                }
                if(ENABLED[3] && mc.options.getCameraType()!=CameraType.FIRST_PERSON) {
                    Vec3 p=mc.player.position();
                    double cx=p.x-cam.x,cy=p.y+mc.player.getBbHeight()+.09-cam.y
                        +Math.sin(t*2)*.013,cz=p.z-cam.z;
                    drawRing(pose,line,cx,cy+.003,cz,.565,
                        VisionGeometry.withAlpha(VisionMenuTheme.PURPLE,210),1.8f,SEGMENTS);
                }
            }
            } catch (RuntimeException ex) {
                // Do not crash the entire server session if a custom renderer
                // rejects this pass. Log once and stop risky geometry.
                renderFaulted=true;
                System.err.println("[VisionMod][WORLD FX] Suspended after render error: "+ex);
                ex.printStackTrace();
            }
        });
    }

    private static void drawRing(PoseStack pose,VertexConsumer line,
        double cx,double cy,double cz,double radius,int color,float width,int steps) {
        for(int i=0;i<steps;i++) {
            double a=i*Math.PI*2/steps,b=(i+1)*Math.PI*2/steps;
            VisionGeometry.line(line,pose,
                cx+Math.cos(a)*radius,cy,cz+Math.sin(a)*radius,
                cx+Math.cos(b)*radius,cy,cz+Math.sin(b)*radius,
                color,width);
        }
    }

    private static void drawBand(VertexConsumer fill,PoseStack pose,
        double cx,double cy,double cz,double outer,double width,int color,int steps) {
        double inner=Math.max(0.001,outer-width);
        for(int i=0;i<steps;i++) {
            double a=i*Math.PI*2/steps,b=(i+1)*Math.PI*2/steps;
            VisionGeometry.quad(fill,pose,
                (float)(cx+Math.cos(a)*outer),(float)cy,(float)(cz+Math.sin(a)*outer),
                (float)(cx+Math.cos(a)*inner),(float)cy,(float)(cz+Math.sin(a)*inner),
                (float)(cx+Math.cos(b)*inner),(float)cy,(float)(cz+Math.sin(b)*inner),
                (float)(cx+Math.cos(b)*outer),(float)cy,(float)(cz+Math.sin(b)*outer),
                color,0,1,0);
        }
    }

    private static void tick(Minecraft mc) {
        if(mc.level==null || mc.player==null) {
            reset(null);return;
        }
        if(previousWorld!=mc.level) reset(mc.level);
        if(!VisionModClient.enabled) {
            TRAIL.clear();landingAge=0;return;
        }
        long now=mc.level.getGameTime();
        if(ENABLED[1]) {
            while(!TRAIL.isEmpty() && (now-TRAIL.peekFirst().time())>TRAIL_LIFETIME)
                TRAIL.removeFirst();
            if(++tick>=2) {
                tick=0;
                Vec3 p=mc.player.position();
                if (!TRAIL.isEmpty() && TRAIL.peekLast().position().distanceToSqr(p)>7.0)
                    TRAIL.clear();
                if(TRAIL.isEmpty() || TRAIL.peekLast().position().distanceToSqr(p)>.012) {
                    TRAIL.addLast(new TrailPoint(p,now));
                    while(TRAIL.size()>MAX_TRAIL) TRAIL.removeFirst();
                }
            }
        } else {
            tick=0;
            TRAIL.clear();
        }
        boolean onGround=mc.player.onGround();
        if(ENABLED[2] && onGround && !grounded && !mc.player.isPassenger()){
            landingPos=mc.player.position();landingAge=20;
        }
        grounded=onGround;
        if(landingAge>0) landingAge--;
    }

    private static void reset(ClientLevel level) {
        previousWorld=level;
        TRAIL.clear();landingPos=null;
        landingAge=tick=0;grounded=false;
    }

    private static void load() {
        if (!Files.isRegularFile(CONFIG)) return;
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(CONFIG)) {
            p.load(in);
            for(int i=0;i<ENABLED.length;i++)
                ENABLED[i]=Boolean.parseBoolean(p.getProperty("effect."+i,"false"));
            try { trailStartColor=parseTrailColor(p.getProperty("trail.start", "#67E8F9")); }
            catch(IllegalArgumentException e){trailStartColor=0xFF67E8F9;}
            try { trailEndColor=parseTrailColor(p.getProperty("trail.end", "#B66DFF")); }
            catch(IllegalArgumentException e){trailEndColor=0xFFB66DFF;}
            try { trailMode=Math.max(0,Math.min(2,Integer.parseInt(p.getProperty("trail.mode","1")))); }
            catch(NumberFormatException e){trailMode=1;}
            try { trailWidth=Math.max(.04f,Math.min(.30f,Float.parseFloat(p.getProperty("trail.width",".115")))); }
            catch(NumberFormatException e){trailWidth=.115f;}
            if(!Float.isFinite(trailWidth))trailWidth=.115f;
            try { trailOpacity=Math.max(30,Math.min(250,Integer.parseInt(p.getProperty("trail.opacity","160")))); }
            catch(NumberFormatException e){trailOpacity=160;}
        }catch(Exception e){System.err.println("[VisionMod] World FX load: "+e);}
    }
    private static void save() {
        Properties p=new Properties();
        for(int i=0;i<ENABLED.length;i++)
            p.setProperty("effect."+i,Boolean.toString(ENABLED[i]));
        p.setProperty("trail.start",trailColorHex(trailStartColor));
        p.setProperty("trail.end",trailColorHex(trailEndColor));
        p.setProperty("trail.mode",Integer.toString(trailMode));
        p.setProperty("trail.width",Float.toString(trailWidth));
        p.setProperty("trail.opacity",Integer.toString(trailOpacity));
        try {
            Files.createDirectories(CONFIG.getParent());
            try(OutputStream out=Files.newOutputStream(CONFIG)) {
                p.store(out,"VisionMod original low-cost smooth world FX");
            }
        }catch(Exception e){System.err.println("[VisionMod] World FX save: "+e);}
    }
}
