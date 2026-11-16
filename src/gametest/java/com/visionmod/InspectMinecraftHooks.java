package com.visionmod;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Prints the actual mapped call sites before launching the integration test. */
public final class InspectMinecraftHooks {
    public static void main(String[] args)throws Exception{
        for(String name:new String[]{"net/minecraft/client/Camera","net/minecraft/client/MouseHandler","net/minecraft/client/gui/Gui"}){
            try(var in=InspectMinecraftHooks.class.getClassLoader().getResourceAsStream(name+".class")){
                if(in==null)throw new IllegalStateException("Missing mapped class "+name);
                ClassNode node=new ClassNode();new ClassReader(in).accept(node,ClassReader.SKIP_DEBUG);
                for(MethodNode method:node.methods){
                    if(!(method.name.equals("setup")||method.name.equals("turnPlayer")||method.name.toLowerCase(java.util.Locale.ROOT).contains("hotbar")))continue;
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
