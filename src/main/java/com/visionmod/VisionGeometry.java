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
    public static final int NO_OVERLAY = 0;

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
