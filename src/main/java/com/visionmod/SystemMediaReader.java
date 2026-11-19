package com.visionmod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** OS adapters run on one worker thread. close() terminates their child process. */
public final class SystemMediaReader implements AutoCloseable {
    private volatile Process process;
    private volatile boolean closed;
    public void read(Consumer<SystemMediaTrack> sink)throws Exception{
        read((track,observedNanos)->sink.accept(track));
    }
    public void read(BiConsumer<SystemMediaTrack,Long> sink)throws Exception{
        if(closed)return;
        String os=System.getProperty("os.name","").toLowerCase(Locale.ROOT);
        if(os.contains("win"))readWindows(sink);
        else if(os.contains("linux"))readLinux(sink);
        else throw new UnsupportedOperationException("System media needs Windows or Linux");
    }
    @Override public void close(){closed=true;Process p=process;if(p!=null)p.destroyForcibly();}
    private void readWindows(BiConsumer<SystemMediaTrack,Long> sink)throws Exception{
        String script;
        try(var in=SystemMediaReader.class.getResourceAsStream("/visionmod/media-session.ps1")){
            if(in==null)throw new java.io.IOException("Missing system media reader");
            script=new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
        String encoded=Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
        // Fixed, bundled script; track metadata is never interpolated into commands.
        String systemRoot=System.getenv("SystemRoot");
        String powershell=(systemRoot==null?"C:\\Windows":systemRoot)+"\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
        if(closed)return;
        process=new ProcessBuilder(powershell,"-NoLogo","-NoProfile","-NonInteractive","-EncodedCommand",encoded)
            .redirectErrorStream(true).start();
        if(closed){close();return;}
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){
            String line;
            while(!closed&&(line=reader.readLine())!=null){
                long observedNanos=System.nanoTime();
                line=line.strip();
                if(line.isEmpty()||line.length()>16384)continue;
                if(!line.startsWith("{"))continue;
                SystemMediaTrack track;
                try{track=decodeWindows(line);}catch(RuntimeException ignored){continue;}
                sink.accept(track,observedNanos);
            }
        }
    }
    private static SystemMediaTrack decodeWindows(String line)throws IOException{
        JsonObject doc=JsonParser.parseString(line).getAsJsonObject();
        if(doc.has("error"))throw new IOException("Windows media API returned an error");
        if(!doc.has("title"))return null;
        return new SystemMediaTrack(text(doc,"provider"),text(doc,"title"),text(doc,"artist"),
            doc.get("positionMs").getAsLong(),doc.get("paused").getAsBoolean(),doc.get("durationMs").getAsLong(),
            doc.get("rate").getAsDouble(),doc.get("positionKnown").getAsBoolean(),text(doc,"url"));
    }
    private static String text(JsonObject doc,String key){
        if(!doc.has(key)||doc.get(key).isJsonNull())return "";
        return doc.get(key).getAsString();
    }
    private String command(String... args)throws IOException,InterruptedException{
        if(closed)return "";
        Process p=new ProcessBuilder(args).redirectError(ProcessBuilder.Redirect.DISCARD).start();process=p;
        if(closed){p.destroyForcibly();return "";}
        // Drain while the command runs so metadata does not get stuck in a full stdout pipe.
        var read=new FutureTask<>(()->p.getInputStream().readNBytes(32769));
        Thread.startVirtualThread(read);
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
        try{
            byte[] output=read.get(2,TimeUnit.SECONDS);
            if(output.length>32768)throw new IOException("Media metadata too large");
            long remaining=Math.max(0,deadline-System.nanoTime());
            if(!p.waitFor(remaining,TimeUnit.NANOSECONDS))throw new IOException("Media command timed out");
            if(p.exitValue()!=0)throw new IOException("System media bus unavailable");
            return new String(output,StandardCharsets.UTF_8);
        }catch(TimeoutException e){throw new IOException("Media command timed out",e);}
        catch(ExecutionException e){throw new IOException("Cannot read media metadata",e.getCause());}
        finally{p.destroyForcibly();read.cancel(true);}
    }
    private void readLinux(BiConsumer<SystemMediaTrack,Long> sink)throws Exception{
        String preferred="";
        while(!closed){
            String names=command("gdbus","call","--session","--dest","org.freedesktop.DBus","--object-path",
                "/org/freedesktop/DBus","--method","org.freedesktop.DBus.ListNames");
            var matches=Pattern.compile("'(org\\.mpris\\.MediaPlayer2\\.[A-Za-z0-9_.-]+)'").matcher(names);
            var players=new LinkedHashSet<String>();
            while(matches.find())players.add(matches.group(1));
            if(players.remove(preferred))players.addFirst(preferred);
            SystemMediaTrack choice=null;
            long observedNanos=System.nanoTime(), deadline=observedNanos+TimeUnit.SECONDS.toNanos(2);
            int count=0;
            for(String source:players){
                if(closed||count++>=8||System.nanoTime()>=deadline)break;
                try{
                    String data=property(source,"GetAll");
                    long sampledAt=System.nanoTime();
                    SystemMediaTrack track=SystemMediaTrack.fromMpris(source,data);
                    if(track.title().isBlank())continue;
                    if(!track.positionKnown()&&System.nanoTime()<deadline){
                        try{
                            long millis=SystemMediaTrack.positionFromMprisProperty(property(source,"Get","Position"));
                            if(millis>=0){track=track.atPosition(millis);sampledAt=System.nanoTime();}
                        }catch(IOException ignored){/* Position may be unsupported by this session. */}
                    }
                    if(choice==null||track.selectionScore()>choice.selectionScore()){
                        choice=track;
                        observedNanos=sampledAt;
                    }
                    if(track.selectionScore()==7)break;
                }catch(IOException ignored){ /* A player may close during enumeration. */ }
            }
            if(choice!=null)preferred=choice.provider();
            if(closed)return;
            sink.accept(choice,observedNanos);
            if(!closed)Thread.sleep(500);
        }
    }
    private String property(String player,String method,String... properties)throws IOException,InterruptedException{
        String[] args={"gdbus","call","--session","--dest",player,"--object-path","/org/mpris/MediaPlayer2",
            "--method","org.freedesktop.DBus.Properties."+method,"org.mpris.MediaPlayer2.Player"};
        int length=args.length;
        args=java.util.Arrays.copyOf(args,length+properties.length);
        System.arraycopy(properties,0,args,length,properties.length);
        return command(args);
    }
}
