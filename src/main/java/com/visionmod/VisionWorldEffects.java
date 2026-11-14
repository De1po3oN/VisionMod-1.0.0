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
    private record TrailPoint(Vec3 position,long time) {}
    private static final Deque<TrailPoint> TRAIL = new ArrayDeque<>(MAX_TRAIL);
    private static ClientLevel previousWorld;
    private static Vec3 landingPos;
    private static int landingAge;
    private static boolean grounded;
    private static int tick;

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
            if (!VisionModClient.enabled || ctx.consumers()==null || ctx.matrices()==null)
                return;
            Minecraft mc=Minecraft.getInstance();
            if(mc.level==null || mc.player==null ||
                (!ENABLED[0] && !ENABLED[1] && !ENABLED[2] && !ENABLED[3])) return;
            Vec3 cam=mc.gameRenderer.getMainCamera().position();
            PoseStack pose=ctx.matrices();
            VertexConsumer line=ctx.consumers().getBuffer(RenderTypes.lines());
            // One shared existing vanilla texture, no extra shader or framebuffers.
            VertexConsumer fill=ctx.consumers().getBuffer(
                RenderTypes.entityTranslucent(VisionGeometry.WHITE_TEXTURE));
            double t=System.nanoTime()*1.0e-9;

            if(ENABLED[0] && mc.hitResult instanceof EntityHitResult hit &&
                hit.getEntity() instanceof LivingEntity living &&
                living!=mc.player && living.isAlive() &&
                living.distanceToSqr(mc.player)<24*24) {
                int c=VisionTools2.hitColor();
                AABB box=living.getBoundingBox().inflate(.035);
                ShapeRenderer.renderShape(pose,line,Shapes.create(box),
                    -cam.x,-cam.y,-cam.z,VisionGeometry.withAlpha(c,210),2f);
                double radius=living.getBbWidth()*.64+.12;
                double cx=living.getX()-cam.x,cy=box.minY-cam.y+.035,cz=living.getZ()-cam.z;
                drawRing(pose,line,cx,cy,cz,radius,VisionGeometry.withAlpha(c,160),1.5f,SEGMENTS);
            }

            if(ENABLED[1] && TRAIL.size()>=2) {
                TrailPoint last=null;
                long worldTime=mc.level.getGameTime();
                int previousColor=VisionMenuTheme.CYAN;
                for(TrailPoint current:TRAIL) {
                    if(last!=null) {
                        Vec3 a=last.position(),b=current.position();
                        double dx=b.x-a.x,dz=b.z-a.z;
                        double length=Math.sqrt(dx*dx+dz*dz);
                        if(length>.02 && length<3.0) {
                            float strength=(float)Math.max(0,1.0-(worldTime-current.time())/(double)TRAIL_LIFETIME);
                            float width=.115f*strength+.018f;
                            double px=-dz/length*width,pz=dx/length*width;
                            int colored=VisionGeometry.blendRgb(previousColor,
                                VisionMenuTheme.PURPLE,(float)((worldTime+current.time())%30)/30f);
                            int alpha=(int)(strength*160);
                            float ax=(float)(a.x-cam.x),az=(float)(a.z-cam.z);
                            float bx=(float)(b.x-cam.x),bz=(float)(b.z-cam.z);
                            float ay=(float)(a.y-cam.y+.09),by=(float)(b.y-cam.y+.09);
                            VisionGeometry.quad(fill,pose,
                                (float)(ax+px),ay,(float)(az+pz),
                                (float)(ax-px),ay,(float)(az-pz),
                                (float)(bx-px),by,(float)(bz-pz),
                                (float)(bx+px),by,(float)(bz+pz),
                                VisionGeometry.withAlpha(colored,alpha),0,1,0);
                            VisionGeometry.line(line,pose,ax,ay,az,bx,by,bz,
                                VisionGeometry.withAlpha(colored,Math.min(210,alpha+45)),1.4f);
                        }
                    }
                    last=current;
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
                drawRing(pose,line,cx,cy+.012,cz,.22+eased*1.05,
                    VisionGeometry.withAlpha(VisionMenuTheme.PURPLE,alpha),1.8f,SEGMENTS);
                if(progress<.7f) drawRing(pose,line,cx,cy+.025,cz,
                    .12+eased*.67,VisionGeometry.withAlpha(VisionMenuTheme.CYAN,
                        (int)(80*(1-progress))),1.1f,SEGMENTS);
            }

            if(ENABLED[3] && mc.options.getCameraType()!=CameraType.FIRST_PERSON) {
                // Conical pastel brim instead of 54 detached floating cubes.
                Vec3 p=mc.player.position();
                double cx=p.x-cam.x,cy=p.y+mc.player.getBbHeight()+.09-cam.y
                    +Math.sin(t*2.0)*.013,cz=p.z-cam.z;
                int a=VisionGeometry.withAlpha(VisionMenuTheme.PURPLE,126);
                int b=VisionGeometry.withAlpha(VisionMenuTheme.CYAN,119);
                for(int i=0;i<SEGMENTS;i++) {
                    double angle=i*2*Math.PI/SEGMENTS;
                    double next=(i+1)*2*Math.PI/SEGMENTS;
                    float x1=(float)(cx+Math.cos(angle)*.56);
                    float z1=(float)(cz+Math.sin(angle)*.56);
                    float x2=(float)(cx+Math.cos(next)*.56);
                    float z2=(float)(cz+Math.sin(next)*.56);
                    // A degenerate quad is one triangle in the quad pipeline.
                    VisionGeometry.quad(fill,pose,
                        (float)cx,(float)(cy+.33),(float)cz,
                        x1,(float)cy,z1,
                        x2,(float)cy,z2,
                        (float)cx,(float)(cy+.33),(float)cz,
                        i%2==0?a:b,0,1,0);
                }
                drawBand(fill,pose,cx,cy-.008,cz,.555,.085,
                    VisionGeometry.withAlpha(VisionMenuTheme.CYAN,120),SEGMENTS);
                drawRing(pose,line,cx,cy+.003,cz,.565,
                    VisionGeometry.withAlpha(VisionMenuTheme.PURPLE,210),1.8f,SEGMENTS);
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
        }catch(Exception e){System.err.println("[VisionMod] World FX load: "+e);}
    }
    private static void save() {
        Properties p=new Properties();
        for(int i=0;i<ENABLED.length;i++)
            p.setProperty("effect."+i,Boolean.toString(ENABLED[i]));
        try {
            Files.createDirectories(CONFIG.getParent());
            try(OutputStream out=Files.newOutputStream(CONFIG)) {
                p.store(out,"VisionMod original low-cost smooth world FX");
            }
        }catch(Exception e){System.err.println("[VisionMod] World FX save: "+e);}
    }
}
