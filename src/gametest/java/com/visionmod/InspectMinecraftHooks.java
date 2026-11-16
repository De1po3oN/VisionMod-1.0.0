package com.visionmod;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Prints the actual mapped call sites before launching the integration test. */
public final class InspectMinecraftHooks {
    public static void main(String[] args)throws Exception{
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
