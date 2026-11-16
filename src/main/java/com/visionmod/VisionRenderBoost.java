package com.visionmod;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.client.CloudStatus;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Reversible, opt-in changes to vanilla rendering workload; not a server-TPS fix.
 * Adaptive mode checks FPS every 10s, with separate downshift/upshift thresholds.
 */
public final class VisionRenderBoost {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("visionmod/render-boost.properties");
    private static final Path BACKUP = FabricLoader.getInstance().getConfigDir().resolve("visionmod/video-backup.properties");
    public static boolean enabled = false;
    public static boolean adaptiveChunks = true;
    public static int chunkCap = 6;
    public static int targetFps = 50;
    private static boolean applied;
    private static long nextRestoreAttempt;
    private static int savedChunks;
    private static double savedEntityScale;
    private static ParticleStatus savedParticles;
    private static CloudStatus savedClouds;
    private static boolean savedEntityShadows;
    private static int savedBiomeBlend;
    private static boolean savedAmbientOcclusion;
    private static boolean savedImprovedTransparency;
    private static boolean savedCutoutLeaves;
    private static int savedWeatherRadius;
    private static int appliedChunks = -1;
    private static int adaptiveCap = 6;
    private static int ticker;
    private static int lowSamples;
    private static int highSamples;
    private static Minecraft mc;

    private VisionRenderBoost() {}
    public static void init() {
        load();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            mc = client;
            // End toggles the entire mod: restore vanilla video options instead
            // of leaving an invisible FPS cap active while Vision Mod is OFF.
            if (!enabled || !VisionModClient.enabled || client.level == null) {
                if (applied || (!enabled || !VisionModClient.enabled) && Files.isRegularFile(BACKUP)) restore(client);
                return;
            }
            if (!applied) { adaptiveCap = chunkCap; lowSamples = highSamples = 0; apply(client); }
            // Only touch expensive video options when the target actually changed.
            if (++ticker >= 200) {
                ticker = 0;
                adjustBudget(client);
                apply(client);
            } else if (appliedChunks > chunkCap) {
                adaptiveCap = chunkCap;
                apply(client);
            }
        });
    }
    public static void toggle() {
        enabled = !enabled;
        save();
        if (!enabled && mc != null && (applied || Files.isRegularFile(BACKUP))) restore(mc);
    }
    public static void cycleCap() {
        chunkCap = chunkCap == 4 ? 6 : chunkCap == 6 ? 8 : 4;
        adaptiveCap = Math.min(adaptiveCap, chunkCap);
        save();
        if (enabled && applied && mc != null) apply(mc);
    }
    public static void cycleTargetFps() {
        targetFps = targetFps == 40 ? 50 : targetFps == 50 ? 60 : targetFps == 60 ? 75 : 40;
        save();
    }
    private static void adjustBudget(Minecraft client) {
        if (!adaptiveChunks || client.screen != null) return;
        int fps = VisionPerformance.estimatedFps();
        if (fps < targetFps - 4) { lowSamples++; highSamples = 0; }
        else if (fps > targetFps + 15) { highSamples++; lowSamples = 0; }
        else { lowSamples = highSamples = 0; }
        // Downshift after ~20s of pressure; recover much more slowly to avoid oscillation.
        if (lowSamples >= 2) {
            adaptiveCap = adaptiveCap > 6 ? 6 : 4;
            lowSamples = 0;
        }
        if (highSamples >= 6) {
            adaptiveCap = Math.min(chunkCap, adaptiveCap == 4 ? 6 : 8);
            highSamples = 0;
        }
    }
    private static void apply(Minecraft client) {
        if (!applied) {
            if (!Files.isRegularFile(BACKUP)) {
                savedChunks = client.options.renderDistance().get();
                savedEntityScale = client.options.entityDistanceScaling().get();
                savedParticles = client.options.particles().get();
                savedClouds = client.options.cloudStatus().get();
                savedEntityShadows = client.options.entityShadows().get();
                savedBiomeBlend = client.options.biomeBlendRadius().get();
                savedAmbientOcclusion = client.options.ambientOcclusion().get();
                savedImprovedTransparency = client.options.improvedTransparency().get();
                savedCutoutLeaves = client.options.cutoutLeaves().get();
                savedWeatherRadius = client.options.weatherRadius().get();
                if (!backup()) { enabled = false; save(); return; }
            } else readBackup(client);
            applied = true;
        }
        int effectiveCap = adaptiveChunks ? Math.min(chunkCap, adaptiveCap) : chunkCap;
        int targetChunks = Math.max(2, Math.min(savedChunks, effectiveCap));
        if (client.options.renderDistance().get() != targetChunks)
            client.options.renderDistance().set(targetChunks);
        appliedChunks = targetChunks;
        // Higher player/entity counts benefit from a more aggressive distance cap.
        double entityScale = Math.min(savedEntityScale, VisionPerformance.busy() ? .35D : .50D);
        if (client.options.entityDistanceScaling().get() != entityScale)
            client.options.entityDistanceScaling().set(entityScale);
        if (client.options.particles().get() != ParticleStatus.MINIMAL)
            client.options.particles().set(ParticleStatus.MINIMAL);
        if (client.options.cloudStatus().get() != CloudStatus.OFF)
            client.options.cloudStatus().set(CloudStatus.OFF);
        if (client.options.entityShadows().get())
            client.options.entityShadows().set(false);
        if (client.options.biomeBlendRadius().get() != 0)
            client.options.biomeBlendRadius().set(0);
        if (client.options.ambientOcclusion().get())
            client.options.ambientOcclusion().set(false);
        if (client.options.improvedTransparency().get())
            client.options.improvedTransparency().set(false);
        if (client.options.cutoutLeaves().get())
            client.options.cutoutLeaves().set(false);
        int weatherCap = Math.min(savedWeatherRadius, 4);
        if (client.options.weatherRadius().get() != weatherCap)
            client.options.weatherRadius().set(weatherCap);
    }
    private static void restore(Minecraft client) {
        if(System.nanoTime()<nextRestoreAttempt)return;
        nextRestoreAttempt=System.nanoTime()+5_000_000_000L;
        try {
            readBackup(client);
            client.options.renderDistance().set(savedChunks);
            client.options.entityDistanceScaling().set(savedEntityScale);
            client.options.particles().set(savedParticles);
            client.options.cloudStatus().set(savedClouds);
            client.options.entityShadows().set(savedEntityShadows);
            client.options.biomeBlendRadius().set(savedBiomeBlend);
            client.options.ambientOcclusion().set(savedAmbientOcclusion);
            client.options.improvedTransparency().set(savedImprovedTransparency);
            client.options.cutoutLeaves().set(savedCutoutLeaves);
            client.options.weatherRadius().set(savedWeatherRadius);
            Files.deleteIfExists(BACKUP);
            nextRestoreAttempt=0;
        } catch (Exception e) {
            System.err.println("[VisionMod] Video settings restore failed; backup retained: " + e);
        } finally {
            applied = false;
            appliedChunks = -1;
            ticker = 0;
            lowSamples = highSamples = 0;
        }
    }
    private static boolean backup() {
        try {
            Files.createDirectories(BACKUP.getParent());
            Properties p = new Properties();
            p.setProperty("chunks",Integer.toString(savedChunks));
            p.setProperty("entities",Double.toString(savedEntityScale));
            p.setProperty("particles",savedParticles.name());
            p.setProperty("clouds",savedClouds.name());
            p.setProperty("entityShadows",Boolean.toString(savedEntityShadows));
            p.setProperty("biomeBlendRadius",Integer.toString(savedBiomeBlend));
            p.setProperty("ambientOcclusion",Boolean.toString(savedAmbientOcclusion));
            p.setProperty("improvedTransparency",Boolean.toString(savedImprovedTransparency));
            p.setProperty("cutoutLeaves",Boolean.toString(savedCutoutLeaves));
            p.setProperty("weatherRadius",Integer.toString(savedWeatherRadius));
            try(OutputStream out=Files.newOutputStream(BACKUP)){p.store(out,"Minecraft video options before Vision boost");}
            return true;
        }catch(Exception e){System.err.println("[VisionMod] Video backup failed: "+e);return false;}
    }
    private static void readBackup(Minecraft client) {
        savedChunks = client.options.renderDistance().get();
        savedEntityScale = client.options.entityDistanceScaling().get();
        savedParticles = client.options.particles().get();
        savedClouds = client.options.cloudStatus().get();
        savedEntityShadows = client.options.entityShadows().get();
        savedBiomeBlend = client.options.biomeBlendRadius().get();
        savedAmbientOcclusion = client.options.ambientOcclusion().get();
        savedImprovedTransparency = client.options.improvedTransparency().get();
        savedCutoutLeaves = client.options.cutoutLeaves().get();
        savedWeatherRadius = client.options.weatherRadius().get();
        if(!Files.isRegularFile(BACKUP))return;
        try(InputStream in=Files.newInputStream(BACKUP)){
            Properties p=new Properties();p.load(in);
            savedChunks=Integer.parseInt(p.getProperty("chunks"));
            savedEntityScale=Double.parseDouble(p.getProperty("entities"));
            savedParticles=ParticleStatus.valueOf(p.getProperty("particles"));
            savedClouds=CloudStatus.valueOf(p.getProperty("clouds"));
            savedEntityShadows=Boolean.parseBoolean(p.getProperty("entityShadows",Boolean.toString(savedEntityShadows)));
            savedBiomeBlend=Integer.parseInt(p.getProperty("biomeBlendRadius",Integer.toString(savedBiomeBlend)));
            savedAmbientOcclusion=Boolean.parseBoolean(p.getProperty("ambientOcclusion",Boolean.toString(savedAmbientOcclusion)));
            savedImprovedTransparency=Boolean.parseBoolean(p.getProperty("improvedTransparency",Boolean.toString(savedImprovedTransparency)));
            savedCutoutLeaves=Boolean.parseBoolean(p.getProperty("cutoutLeaves",Boolean.toString(savedCutoutLeaves)));
            savedWeatherRadius=Integer.parseInt(p.getProperty("weatherRadius",Integer.toString(savedWeatherRadius)));
        }catch(Exception e){System.err.println("[VisionMod] Invalid saved video backup: "+e);}
    }
    public static void load() {
        if(!Files.isRegularFile(FILE))return;
        try(InputStream in=Files.newInputStream(FILE)){
            Properties p=new Properties();p.load(in);
            enabled=Boolean.parseBoolean(p.getProperty("enabled",Boolean.toString(enabled)));
            adaptiveChunks=Boolean.parseBoolean(p.getProperty("adaptiveChunks","true"));
            int cap=Integer.parseInt(p.getProperty("chunkCap","6"));
            chunkCap=cap==4||cap==6||cap==8?cap:6;
            int fps=Integer.parseInt(p.getProperty("targetFps","50"));
            targetFps=fps==40||fps==50||fps==60||fps==75?fps:50;
            adaptiveCap=chunkCap;
        }catch(Exception e){System.err.println("[VisionMod] Failed reading render boost: "+e);}
    }
    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Properties p=new Properties();
            p.setProperty("enabled",Boolean.toString(enabled));
            p.setProperty("chunkCap",Integer.toString(chunkCap));
            p.setProperty("targetFps",Integer.toString(targetFps));
            p.setProperty("adaptiveChunks",Boolean.toString(adaptiveChunks));
            try(OutputStream out=Files.newOutputStream(FILE)){p.store(out,"Vision Mod 1.0.0 actual rendering workload limits");}
        }catch(Exception e){System.err.println("[VisionMod] Cannot save render boost: "+e);}
    }
}
