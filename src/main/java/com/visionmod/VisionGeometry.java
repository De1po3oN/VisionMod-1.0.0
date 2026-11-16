package com.visionmod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Original compact geometrical drawing helpers for optional world FX.
 * Uses an original low-cost pearlescent texture, no copied third-party assets,
 * external downloads, allocations per quad, or direct OpenGL state changes.
 */
public final class VisionGeometry {
    public static final Identifier WHITE_TEXTURE =
        Identifier.fromNamespaceAndPath("visionmod", "textures/entity/pet_surface.png");
    /** Separate fur texture so the pet has paw/star patterns without coloring other FX. */
    public static final Identifier PET_FUR =
        Identifier.fromNamespaceAndPath("visionmod", "textures/entity/pet_fur.png");
    public static final int FULL_BRIGHT = 0xF000F0;
    public static final int NO_OVERLAY = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

    private VisionGeometry() {}

    public static void vertex(VertexConsumer out, PoseStack pose, float x, float y, float z,
                              float u, float v, int color, float nx, float ny, float nz) {
        out.addVertex(pose.last(), x, y, z)
            .setColor(color).setUv(u, v)
            .setOverlay(NO_OVERLAY).setLight(FULL_BRIGHT)
            .setNormal(pose.last(), nx, ny, nz);
    }

    public static void quad(VertexConsumer out, PoseStack pose,
                            float ax, float ay, float az,
                            float bx, float by, float bz,
                            float cx, float cy, float cz,
                            float dx, float dy, float dz,
                            int color, float nx, float ny, float nz) {
        vertex(out,pose,ax,ay,az,0,0,color,nx,ny,nz);
        vertex(out,pose,bx,by,bz,1,0,color,nx,ny,nz);
        vertex(out,pose,cx,cy,cz,1,1,color,nx,ny,nz);
        vertex(out,pose,dx,dy,dz,0,1,color,nx,ny,nz);
    }

    public static void cube(VertexConsumer out, PoseStack pose,
                            float x0,float y0,float z0,float x1,float y1,float z1,int color) {
        // 6 faces; no dynamic entity models or GPU buffers per cube.
        quad(out,pose,x0,y0,z0,x0,y1,z0,x1,y1,z0,x1,y0,z0,color,0,0,-1);
        quad(out,pose,x1,y0,z1,x1,y1,z1,x0,y1,z1,x0,y0,z1,color,0,0,1);
        quad(out,pose,x0,y0,z1,x0,y1,z1,x0,y1,z0,x0,y0,z0,color,-1,0,0);
        quad(out,pose,x1,y0,z0,x1,y1,z0,x1,y1,z1,x1,y0,z1,color,1,0,0);
        quad(out,pose,x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0,color,0,1,0);
        quad(out,pose,x0,y0,z1,x0,y0,z0,x1,y0,z0,x1,y0,z1,color,0,-1,0);
    }

    public static void line(VertexConsumer out, PoseStack pose,
                            double ax,double ay,double az,double bx,double by,double bz,
                            int color,float width) {
        float dx=(float)(bx-ax),dy=(float)(by-ay),dz=(float)(bz-az);
        float length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(length<1.0e-5f)return;
        dx/=length;dy/=length;dz/=length;
        out.addVertex(pose.last(),(float)ax,(float)ay,(float)az)
            .setColor(color).setNormal(pose.last(),dx,dy,dz).setLineWidth(width);
        out.addVertex(pose.last(),(float)bx,(float)by,(float)bz)
            .setColor(color).setNormal(pose.last(),dx,dy,dz).setLineWidth(width);
    }

    public static void band(VertexConsumer out,PoseStack pose,double x,double y,double z,
                            double radius,double width,int innerColor,int outerColor,int steps){
        for(int i=0;i<steps;i++){
            double a=i*Math.PI*2/steps,b=(i+1)*Math.PI*2/steps;
            double inner=Math.max(.001,radius-width);
            vertex(out,pose,(float)(x+Math.cos(a)*inner),(float)y,(float)(z+Math.sin(a)*inner),0,0,innerColor,0,1,0);
            vertex(out,pose,(float)(x+Math.cos(b)*inner),(float)y,(float)(z+Math.sin(b)*inner),1,0,innerColor,0,1,0);
            vertex(out,pose,(float)(x+Math.cos(b)*radius),(float)y,(float)(z+Math.sin(b)*radius),1,1,outerColor,0,1,0);
            vertex(out,pose,(float)(x+Math.cos(a)*radius),(float)y,(float)(z+Math.sin(a)*radius),0,1,outerColor,0,1,0);
            // Reverse face: the effect stays visible when viewed from below.
            vertex(out,pose,(float)(x+Math.cos(a)*radius),(float)y,(float)(z+Math.sin(a)*radius),0,1,outerColor,0,-1,0);
            vertex(out,pose,(float)(x+Math.cos(b)*radius),(float)y,(float)(z+Math.sin(b)*radius),1,1,outerColor,0,-1,0);
            vertex(out,pose,(float)(x+Math.cos(b)*inner),(float)y,(float)(z+Math.sin(b)*inner),1,0,innerColor,0,-1,0);
            vertex(out,pose,(float)(x+Math.cos(a)*inner),(float)y,(float)(z+Math.sin(a)*inner),0,0,innerColor,0,-1,0);
        }
    }
    private static final java.util.Map<Integer,float[]> SPHERES=new java.util.LinkedHashMap<>();
    public static void ellipsoid(VertexConsumer out,PoseStack pose,float x,float y,float z,
                                 float rx,float ry,float rz,int color,int slices,int rings){
        slices=Math.clamp(slices,4,24);rings=Math.clamp(rings,3,12);
        int key=slices*32+rings;
        float[] mesh=SPHERES.get(key);
        if(mesh==null){
            mesh=sphereMesh(slices,rings);
            if(SPHERES.size()>=12)SPHERES.remove(SPHERES.keySet().iterator().next());
            SPHERES.put(key,mesh);
        }
        for(int i=0;i<mesh.length;i+=5){
            float nx=mesh[i],ny=mesh[i+1],nz=mesh[i+2];
            vertex(out,pose,x+rx*nx,y+ry*ny,z+rz*nz,mesh[i+3],mesh[i+4],color,nx,ny,nz);
        }
    }
    private static float[] sphereMesh(int slices,int rings){
        float[] mesh=new float[slices*rings*4*5];int cursor=0;
        for(int row=0;row<rings;row++)for(int col=0;col<slices;col++)for(int corner=0;corner<4;corner++){
            double latitude=-Math.PI/2+(row+(corner==1||corner==2?1:0))*Math.PI/rings;
            double longitude=(col+(corner>=2?1:0))*Math.PI*2/slices;
            mesh[cursor++]=(float)(Math.cos(latitude)*Math.cos(longitude));
            mesh[cursor++]=(float)Math.sin(latitude);
            mesh[cursor++]=(float)(Math.cos(latitude)*Math.sin(longitude));
            mesh[cursor++]=(float)(longitude/(Math.PI*2));mesh[cursor++]=(float)(.5-latitude/Math.PI);
        }
        return mesh;
    }

    public static int withAlpha(int rgb, int alpha) {
        return ((Math.max(0,Math.min(255,alpha)))<<24)|(rgb&0x00ffffff);
    }

    public static int blendRgb(int a,int b,float amount) {
        float t=Math.max(0,Math.min(1,amount));
        int ar=(a>>16)&255, ag=(a>>8)&255, ab=a&255;
        int br=(b>>16)&255, bg=(b>>8)&255, bb=b&255;
        return 0xff000000
            | ((int)(ar+(br-ar)*t)<<16)
            | ((int)(ag+(bg-ag)*t)<<8)
            | (int)(ab+(bb-ab)*t);
    }
}
