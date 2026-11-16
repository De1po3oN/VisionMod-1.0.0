package com.visionmod;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Native OS media metadata. No browser extension, pairing token or listening port. */
public final class VisionMediaSession {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor(r->{
        Thread t=new Thread(r,"VisionMod-SystemMedia");t.setDaemon(true);return t;
    });
    private static volatile SystemMediaReader reader;
    private static volatile int generation;
    private static volatile boolean running;
    private static volatile String status="Включи World Lyrics";
    private static long retryAt;
    private static boolean wasEnabled;
    private VisionMediaSession(){}
    public static String state(){return status;}
    public static void init(){
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            boolean on=VisionModClient.enabled&&VisionExtendedFeatures.enabled(3);
            if(!on&&wasEnabled)stop();
            wasEnabled=on;
            if(!on)return;
            if(!running&&System.nanoTime()>=retryAt){
                retryAt=System.nanoTime()+15_000_000_000L;
                running=true;
                int current=++generation;
                WORKER.execute(()->read(current));
            }
            VisionLyricsLookup.tick();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc->{stop();WORKER.shutdownNow();});
    }
    private static boolean active(int current){return current==generation&&!Thread.currentThread().isInterrupted();}
    private static void stop(){
        generation++;
        SystemMediaReader currentReader=reader;if(currentReader!=null)currentReader.close();
        running=false;status="World Lyrics выключен";
        VisionLyricsSource.clearMediaTrack();VisionLyricsLookup.clear();
    }
    private static void read(int current){
        if(!active(current))return;
        try(SystemMediaReader nativeReader=new SystemMediaReader()){
            reader=nativeReader;
            if(!active(current))return;
            nativeReader.read(track->publish(current,track));
            if(active(current))status="Медиасессия недоступна · повторим автоматически";
        }catch(UnsupportedOperationException e){
            if(active(current))status="Системное чтение музыки доступно на Windows и Linux";
        }catch(Exception e){
            if(active(current))status="Медиасессия недоступна · включи музыку в плеере";
        }finally{
            reader=null;
            if(current==generation)running=false;
        }
    }
    private static void publish(int current,SystemMediaTrack track){
        Minecraft.getInstance().execute(()->{
            if(!active(current))return;
            if(track==null||track.title().isBlank()){
                status="Включи музыку в браузере или плеере";VisionLyricsSource.clearMediaTrack();return;
            }
            status=track.paused()?"Музыка на паузе":"Музыка подключена";
            VisionLyricsSource.receiveMediaTrack(track.provider(),track.artist(),track.title(),track.positionMs(),track.paused());
        });
    }
}
