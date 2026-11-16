package com.visionmod;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

/** Rounded, animated local companion. One texture and consumer for every part. */
public final class VisionPetRenderer {
    private static boolean renderFaulted;
    private static long renderedFrames;
    private static final PetMotion MOTION=new PetMotion();
    private static ClientLevel trackedWorld;
    private static LocalPlayer trackedPlayer;
    private static CameraType previousView;
    private static long previousFrame;
    private static PetMotion.Pose lastPose;
    private static float drawOpacity=1;
    static long renderedFrames(){return renderedFrames;}
    static PetMotion.Pose lastPose(){return lastPose;}
    public static boolean suspended(){return renderFaulted;}
    public static void retryAfterToggle(){renderFaulted=false;resetMotion();}
    private static void resetMotion(){MOTION.reset();previousFrame=0;previousView=null;lastPose=null;}
    private VisionPetRenderer(){}
    public static void init(){
        WorldRenderEvents.AFTER_ENTITIES.register(ctx->{
            Minecraft mc=Minecraft.getInstance();
            if(!VisionModClient.enabled||!VisionExtendedFeatures.enabled(4)||mc.player==null||mc.level==null){resetMotion();return;}
            if(renderFaulted||ctx.matrices()==null||ctx.consumers()==null)return;
            if(trackedWorld!=mc.level||trackedPlayer!=mc.player){trackedWorld=mc.level;trackedPlayer=mc.player;resetMotion();}
            float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            double t=VisionAppearance.animations?(mc.player.tickCount+partial)*.05:0;
            var camera=mc.gameRenderer.getMainCamera();
            Vec3 owner=mc.player.getPosition(partial),cam=camera.position();
            long now=System.nanoTime();double elapsed=previousFrame==0?0:(now-previousFrame)/1_000_000_000.0;previousFrame=now;
            CameraType view=mc.options.getCameraType();
            lastPose=MOTION.sample(owner.x,owner.y,owner.z,camera.yRot(),elapsed,t,VisionAppearance.animations,view!=previousView);previousView=view;
            if(lastPose==null)return;
            Vec3 forward=Vec3.directionFromRotation(camera.xRot(),camera.yRot());
            drawOpacity=lastPose.visibility()*PetMotion.cameraOpacity(lastPose.x()-cam.x,lastPose.y()-cam.y,lastPose.z()-cam.z,forward.x,forward.y,forward.z);
            if(drawOpacity<.02f)return;
            PoseStack pose=ctx.matrices();pose.pushPose();
            try{
                pose.translate(lastPose.x()-cam.x,lastPose.y()-cam.y,lastPose.z()-cam.z);
                pose.mulPose(Axis.YP.rotationDegrees(-lastPose.facingYaw()));
                pose.mulPose(Axis.ZP.rotationDegrees(lastPose.roll()));
                VertexConsumer out=ctx.consumers().getBuffer(RenderTypes.entityTranslucent(VisionGeometry.WHITE_TEXTURE));
                int fur=VisionExtendedFeatures.petColor(),light=EffectMath.blend(fur,0xFFFFFFFF,.62f);
                int shadow=EffectMath.blend(fur,0xFF65568C,.25f),eye=0xFF202138;
                int slices=VisionAppearance.detail==0?10:14,rings=VisionAppearance.detail==0?6:8;
                ball(out,pose,0,-.10f,.015f,.17f,.21f,.14f,shadow,slices,rings);
                ball(out,pose,0,.16f,0,.235f,.205f,.19f,fur,slices,rings);
                ball(out,pose,-.15f,.34f,.015f,.070f,.14f,.068f,fur,8,5);
                ball(out,pose,.15f,.34f,.015f,.070f,.14f,.068f,fur,8,5);
                ball(out,pose,-.15f,.345f,-.048f,.033f,.082f,.018f,0xFFFFBFDA,6,4);
                ball(out,pose,.15f,.345f,-.048f,.033f,.082f,.018f,0xFFFFBFDA,6,4);
                ball(out,pose,0,.11f,-.151f,.165f,.115f,.051f,light,10,5);
                ball(out,pose,-.088f,.18f,-.181f,.030f,.042f,.014f,eye,6,5);
                ball(out,pose,.088f,.18f,-.181f,.030f,.042f,.014f,eye,6,5);
                ball(out,pose,-.079f,.195f,-.194f,.009f,.012f,.006f,0xFFFFFFFF,5,3);
                ball(out,pose,.097f,.195f,-.194f,.009f,.012f,.006f,0xFFFFFFFF,5,3);
                ball(out,pose,0,.10f,-.204f,.019f,.014f,.009f,0xFFD67AAB,6,4);
                ball(out,pose,-.148f,.105f,-.176f,.030f,.018f,.007f,0xFFFFBEDB,6,4);
                ball(out,pose,.148f,.105f,-.176f,.030f,.018f,.007f,0xFFFFBEDB,6,4);
                ball(out,pose,0,-.10f,-.111f,.105f,.123f,.032f,light,8,5);
                ball(out,pose,-.09f,-.285f,-.04f,.066f,.045f,.078f,fur,8,4);
                ball(out,pose,.09f,-.285f,-.04f,.066f,.045f,.078f,fur,8,4);
                float tail=(float)Math.sin(t*2.7)*.065f;
                ball(out,pose,.18f+tail,-.07f,.15f,.075f,.15f,.065f,light,8,5);
                float flutter=(float)Math.sin(t*4)*.025f;
                ball(out,pose,-.235f-flutter,-.025f,.065f,.115f,.055f,.022f,0xC3D8C9FF,8,4);
                ball(out,pose,.235f+flutter,-.025f,.065f,.115f,.055f,.022f,0xC3D8C9FF,8,4);
                VisionGeometry.band(out,pose,0,-.35,0,.30,.028,VisionGeometry.withAlpha(fur,(int)(85*drawOpacity)),VisionGeometry.withAlpha(fur,0),20);
                renderedFrames++;
            }catch(RuntimeException e){renderFaulted=true;System.err.println("[VisionMod][PET] Renderer suspended: "+e);}
            finally{pose.popPose();}
        });
    }
    private static void ball(VertexConsumer out,PoseStack pose,float x,float y,float z,float rx,float ry,float rz,int color,int slices,int rings){
        VisionGeometry.ellipsoid(out,pose,x,y,z,rx,ry,rz,VisionGeometry.withAlpha(color,(int)((color>>>24)*drawOpacity)),slices,rings);
    }
}
