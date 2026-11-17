package com.visionmod;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Decodes MPRIS metadata produced by the system gdbus tool, not a shell command. */
public record SystemMediaTrack(String provider,String title,String artist,long positionMs,boolean paused,
                               long durationMs,double playbackRate,boolean positionKnown,String sourceUrl) {
    public SystemMediaTrack(String provider,String title,String artist,long positionMs,boolean paused){
        this(provider,title,artist,positionMs,paused,0,1,true,"");
    }
    public SystemMediaTrack(String provider,String title,String artist,long positionMs,boolean paused,long durationMs,double playbackRate,boolean positionKnown){
        this(provider,title,artist,positionMs,paused,durationMs,playbackRate,positionKnown,"");
    }
    public SystemMediaTrack {
        provider=clean(provider);title=clean(title);artist=clean(artist);sourceUrl=clean(sourceUrl);
        positionMs=Math.clamp(positionMs,0,86_400_000L);
        durationMs=Math.clamp(durationMs,0,86_400_000L);
        playbackRate=Double.isFinite(playbackRate)&&playbackRate>0?Math.clamp(playbackRate,.25,4):1;
    }
    public boolean isYandex(){
        String app=provider.toLowerCase(java.util.Locale.ROOT);
        return app.matches("(?s).*(?:yandex[.a-z0-9_-]*music|music[.a-z0-9_-]*yandex).*")||
            sourceUrl.toLowerCase(java.util.Locale.ROOT).matches("https?://music\\.yandex\\.[a-z.]+/.*")||
            title.toLowerCase(java.util.Locale.ROOT).matches("(?s).*(?:яндекс[ .]?музыка|yandex music)\\s*$");
    }
    public String serviceName(){return isYandex()?"Яндекс Музыка":provider.toLowerCase(java.util.Locale.ROOT).contains("spotify")?"Spotify":"Системный плеер";}
    public int selectionScore(){return (paused?0:4)+(isYandex()?1:0);}
    private static String clean(String value){
        if(value==null)return "";
        value=value.replaceAll("[\\p{Cntrl}§]"," ").strip();
        return value.substring(0,Math.min(140,value.length()));
    }
    private static final String QUOTED="(?:'((?:\\\\.|[^'\\\\])*)'|\"((?:\\\\.|[^\"\\\\])*)\")";
    public static SystemMediaTrack fromMpris(String source,String data) {
        String title=property(data,"xesam:title",false);
        String artist=property(data,"xesam:artist",true);
        String status=property(data,"PlaybackStatus",false);
        Matcher pos=Pattern.compile("['\"]Position['\"]\\s*:\\s*<(?:int64\\s+|uint64\\s+)?(\\d+)>").matcher(data);
        long millis=0;boolean known=pos.find();
        if(known)try{millis=Long.parseLong(pos.group(1))/1000;}catch(NumberFormatException ignored){known=false;}
        long duration=(long)(number(data,"mpris:length",0)/1000);
        return new SystemMediaTrack(source,title,artist,millis,!status.equals("Playing"),duration,number(data,"Rate",1),known,property(data,"xesam:url",false));
    }
    private static double number(String data,String key,double fallback){
        Matcher m=Pattern.compile("['\"]"+Pattern.quote(key)+"['\"]\\s*:\\s*<(?:int64\\s+|uint64\\s+|double\\s+)?(-?\\d+(?:\\.\\d+)?)>").matcher(data);
        if(m.find())try{return Double.parseDouble(m.group(1));}catch(NumberFormatException ignored){}
        return fallback;
    }
    private static String property(String data,String key,boolean array) {
        Matcher m=Pattern.compile("['\"]"+Pattern.quote(key)+"['\"]\\s*:\\s*<"+(array?"\\[\\s*":"")+QUOTED).matcher(data);
        return m.find()?unescape(m.group(1)!=null?m.group(1):m.group(2)):"";
    }
    private static String unescape(String input) {
        StringBuilder text=new StringBuilder();
        for(int i=0;i<input.length();i++){
            char c=input.charAt(i);
            if(c=='\\'&&i+1<input.length()){
                c=input.charAt(++i);
                if(c=='n'||c=='r'||c=='t')c=' ';
            }
            if(!Character.isISOControl(c)&&c!='§')text.append(c);
        }
        return text.toString();
    }
}
