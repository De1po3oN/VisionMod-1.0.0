package com.mojang.blaze3d.vertex;
public interface VertexConsumer {
    VertexConsumer addVertex(PoseStack.Pose pose,float x,float y,float z);
    VertexConsumer setColor(int color);VertexConsumer setUv(float u,float v);
    VertexConsumer setOverlay(int value);VertexConsumer setLight(int value);
    VertexConsumer setNormal(PoseStack.Pose pose,float x,float y,float z);
    VertexConsumer setLineWidth(float width);
}
