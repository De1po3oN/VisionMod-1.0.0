package com.visionmod;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Prints the actual mapped call sites before launching the integration test. */
public final class InspectMinecraftHooks {
    public static void main(String[] args)throws Exception{
        for(String name:new String[]{"net/minecraft/world/entity/player/Player","net/minecraft/client/player/LocalPlayer","net/minecraft/client/multiplayer/MultiPlayerGameMode","net/minecraft/client/multiplayer/ClientPacketListener","net/minecraft/network/protocol/game/ClientboundSetEntityMotionPacket"}){
            try(var in=InspectMinecraftHooks.class.getClassLoader().getResourceAsStream(name+".class")){
                ClassNode node=new ClassNode();new ClassReader(in).accept(node,ClassReader.SKIP_DEBUG);
                for(MethodNode method:node.methods){
                    String lower=method.name.toLowerCase(java.util.Locale.ROOT);
                    if(!(lower.contains("lunge")||lower.contains("pierc")||lower.contains("setentitymotion")||name.endsWith("ClientboundSetEntityMotionPacket")))continue;
                    System.out.println("[SPEAR_API] "+name+"."+method.name+method.desc);
                    if(name.endsWith("ClientboundSetEntityMotionPacket"))continue;
                    for(AbstractInsnNode instruction:method.instructions){
                        if(instruction instanceof MethodInsnNode call)System.out.println("  call "+call.owner+"."+call.name+call.desc);
                        else if(instruction instanceof FieldInsnNode field)System.out.println("  field "+field.owner+"."+field.name+" "+field.desc);
                        else if(instruction instanceof LdcInsnNode constant)System.out.println("  constant "+constant.cst);
                        else if(instruction instanceof JumpInsnNode jump)System.out.println("  branch "+jump.getOpcode());
                    }
                }
            }
        }
        for(String name:new String[]{"net/minecraft/world/entity/player/Player","net/minecraft/client/multiplayer/ClientPacketListener","net/minecraft/network/protocol/game/ClientboundDamageEventPacket","net/minecraft/network/chat/Style","net/minecraft/network/chat/FontDescription","net/minecraft/core/component/DataComponents"}){
            try(var in=InspectMinecraftHooks.class.getClassLoader().getResourceAsStream(name+".class")){
                if(in==null){System.out.println("[REFERENCE_API] missing "+name);continue;}
                ClassNode node=new ClassNode();new ClassReader(in).accept(node,ClassReader.SKIP_DEBUG);
                for(FieldNode field:node.fields)if(name.endsWith("DataComponents")||name.endsWith("FontDescription"))System.out.println("[REFERENCE_API] "+name+"."+field.name+" "+field.desc);
                for(MethodNode method:node.methods){
                    if(name.endsWith("Player")&&!method.name.toLowerCase(java.util.Locale.ROOT).matches(".*(lunge|spear|pierc|attack|item).*"))continue;
                    if(name.endsWith("ClientPacketListener")&&!method.name.toLowerCase(java.util.Locale.ROOT).contains("damage"))continue;
                    if(name.endsWith("Style")&&!method.name.toLowerCase(java.util.Locale.ROOT).contains("font"))continue;
                    if(name.endsWith("DataComponents"))continue;
                    System.out.println("[REFERENCE_API] "+name+"."+method.name+method.desc);
                }
            }
        }
        for(String name:new String[]{"net/minecraft/client/gui/Font","net/minecraft/client/Camera","net/minecraft/client/renderer/SubmitNodeCollector"}){
            try(var in=InspectMinecraftHooks.class.getClassLoader().getResourceAsStream(name+".class")){
                ClassNode node=new ClassNode();new ClassReader(in).accept(node,0);
                for(MethodNode method:node.methods){
                    if(!(method.name.equals("drawInBatch")||method.name.equals("drawInternal")||method.name.equals("setRotation")||name.endsWith("SubmitNodeCollector")))continue;
                    System.out.println("[TEXT_HOOKS] "+name+"."+method.name+method.desc);
                    if(name.endsWith("SubmitNodeCollector"))continue;
                    for(AbstractInsnNode instruction:method.instructions){
                        if(instruction instanceof MethodInsnNode call)System.out.println("  call "+call.owner+"."+call.name+call.desc);
                        else if(instruction instanceof LdcInsnNode constant)System.out.println("  constant "+constant.cst);
                        else if(instruction instanceof FieldInsnNode field)System.out.println("  field "+field.owner+"."+field.name+" "+field.desc);
                    }
                }
            }
        }
        for(String name:new String[]{"net/minecraft/client/Camera","net/minecraft/client/MouseHandler","net/minecraft/client/gui/Gui","net/minecraft/client/gui/screens/inventory/AbstractContainerScreen","net/minecraft/client/KeyMapping"}){
            try(var in=InspectMinecraftHooks.class.getClassLoader().getResourceAsStream(name+".class")){
                if(in==null)throw new IllegalStateException("Missing mapped class "+name);
                ClassNode node=new ClassNode();new ClassReader(in).accept(node,ClassReader.SKIP_DEBUG);
                for(MethodNode method:node.methods){
                    if(!(method.name.equals("setup")||method.name.equals("turnPlayer")||method.desc.equals("(DD)Lnet/minecraft/world/inventory/Slot;")||method.name.equals("isHovering")||method.name.equals("matches")||method.name.toLowerCase(java.util.Locale.ROOT).contains("hotbar")))continue;
                    System.out.println("[HOOKS] "+name+"."+method.name+method.desc);
                    for(AbstractInsnNode instruction:method.instructions){
                        if(instruction instanceof MethodInsnNode call)System.out.println("  call "+call.owner+"."+call.name+call.desc);
                        else if(instruction instanceof FieldInsnNode field&&field.owner.equals(name))System.out.println("  field "+field.name+" "+field.desc);
                    }
                }
            }
        }
    }
}
