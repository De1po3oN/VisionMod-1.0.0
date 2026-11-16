package net.minecraft.resources;
public record Identifier(String namespace,String path) {public static Identifier fromNamespaceAndPath(String a,String b){return new Identifier(a,b);}}
