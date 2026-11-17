package com.visionmod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** OS adapters run on one worker thread. close() terminates their child process. */
public final class SystemMediaReader implements AutoCloseable {
    private volatile Process process;
    private volatile boolean closed;
    private Consumer<SystemMediaTrack> sink;
    public void read(Consumer<SystemMediaTrack> sink)throws Exception{
        this.sink=sink;
        if(closed)return;
        String os=System.getProperty("os.name","").toLowerCase(Locale.ROOT);
        if(os.contains("win"))readWindows();
        else if(os.contains("linux"))readLinux();
        else throw new UnsupportedOperationException("System media needs Windows or Linux");
    }
    @Override public void close(){closed=true;Process p=process;if(p!=null)p.destroyForcibly();}
    private void readWindows()throws Exception{
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
                line=line.strip();
                if(line.isEmpty()||line.length()>16384)continue;
                if(!line.startsWith("{"))continue;
                JsonObject doc=JsonParser.parseString(line).getAsJsonObject();
                if(doc.has("error"))throw new java.io.IOException("Windows media API returned an error");
                if(!doc.has("title")){sink.accept(null);continue;}
                sink.accept(new SystemMediaTrack(text(doc,"provider"),text(doc,"title"),text(doc,"artist"),
                    doc.get("positionMs").getAsLong(),doc.get("paused").getAsBoolean(),
                    doc.has("durationMs")?doc.get("durationMs").getAsLong():0,
                    doc.has("rate")?doc.get("rate").getAsDouble():1,
                    !doc.has("positionKnown")||doc.get("positionKnown").getAsBoolean(),text(doc,"url")));
            }
        }
    }
    private static String text(JsonObject doc,String key){
        if(!doc.has(key)||doc.get(key).isJsonNull())return "";
        String value=doc.get(key).getAsString().replaceAll("[\\p{Cntrl}§]"," ").strip();
        return value.substring(0,Math.min(value.length(),140));
    }
    private String command(String... args)throws Exception{
        if(closed)return "";
        Process p=new ProcessBuilder(args).redirectError(ProcessBuilder.Redirect.DISCARD).start();process=p;
        if(closed){p.destroyForcibly();return "";}
        try{
            if(!p.waitFor(2,TimeUnit.SECONDS))throw new java.io.IOException("Media command timed out");
            if(p.exitValue()!=0)throw new java.io.IOException("System media bus unavailable");
            byte[] output=p.getInputStream().readNBytes(32769);
            if(output.length>32768)throw new java.io.IOException("Media metadata too large");
            return new String(output,StandardCharsets.UTF_8);
        }finally{p.destroyForcibly();}
    }
    private void readLinux()throws Exception{
        while(!closed){
            String names=command("gdbus","call","--session","--dest","org.freedesktop.DBus","--object-path",
                "/org/freedesktop/DBus","--method","org.freedesktop.DBus.ListNames");
            var matches=Pattern.compile("'(org\\.mpris\\.MediaPlayer2\\.[A-Za-z0-9_.-]+)'").matcher(names);
            SystemMediaTrack choice=null;int count=0;
            while(matches.find()&&count++<8){
                String source=matches.group(1);
                try{
                    String data=command("gdbus","call","--session","--dest",source,"--object-path",
                        "/org/mpris/MediaPlayer2","--method","org.freedesktop.DBus.Properties.GetAll","org.mpris.MediaPlayer2.Player");
                    SystemMediaTrack track=SystemMediaTrack.fromMpris(source,data);
                    if(track.title().isBlank())continue;
                    if(choice==null||track.selectionScore()>choice.selectionScore())choice=track;
                    if(track.selectionScore()==5)break;
                }catch(java.io.IOException ignored){ /* A player may close during enumeration. */ }
            }
            sink.accept(choice);Thread.sleep(500);
        }
    }
}
