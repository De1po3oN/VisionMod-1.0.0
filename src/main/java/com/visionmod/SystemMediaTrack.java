package com.visionmod;

import java.net.URI;
import java.util.Locale;
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
        provider=clean(provider,140);title=clean(title,140);artist=clean(artist,140);sourceUrl=clean(sourceUrl,2048);
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
    public int selectionScore(){return (paused?0:4)+(positionKnown?2:0)+(isYandex()?1:0);}
    private static String clean(String value,int maxCodePoints){
        if(value==null)return "";
        value=value.replaceAll("[\\p{Cntrl}§]"," ").strip();
        int length=Math.min(maxCodePoints,value.codePointCount(0,value.length()));
        return value.substring(0,value.offsetByCodePoints(0,length));
    }
    private static final String QUOTED="(?:'((?:\\\\.|[^'\\\\])*)'|\"((?:\\\\.|[^\"\\\\])*)\")";
    public static SystemMediaTrack fromMpris(String source,String data) {
        String url=property(data,"xesam:url",false);
        String title=property(data,"xesam:title",false);
        if(title.isBlank())title=localFileTitle(url);
        String artist=property(data,"xesam:artist",true);
        if(artist.isBlank())artist=property(data,"xesam:albumArtist",true);
        String status=property(data,"PlaybackStatus",false);
        if(status.equals("Stopped"))title="";
        Matcher pos=Pattern.compile("['\"]Position['\"]\\s*:\\s*<(?:int64\\s+|uint64\\s+)?(\\d+)>").matcher(data);
        long millis=0;boolean known=pos.find();
        if(known)try{millis=Long.parseLong(pos.group(1))/1000;}catch(NumberFormatException ignored){known=false;}
        long duration=(long)(number(data,"mpris:length",0)/1000);
        return new SystemMediaTrack(source,title,artist,millis,!status.equals("Playing"),duration,number(data,"Rate",1),known,url);
    }
    static long positionFromMprisProperty(String data){
        Matcher position=Pattern.compile("^\\s*\\(<(?:int64\\s+|uint64\\s+)?(\\d+)>,\\)\\s*$").matcher(data);
        if(position.matches())try{return Long.parseLong(position.group(1))/1000;}catch(NumberFormatException ignored){}
        return -1;
    }
    SystemMediaTrack atPosition(long millis){
        return new SystemMediaTrack(provider,title,artist,millis,paused,durationMs,playbackRate,true,sourceUrl);
    }
    private static String localFileTitle(String url){
        try{
            URI uri=URI.create(url);
            if(!"file".equalsIgnoreCase(uri.getScheme())||uri.getPath()==null)return "";
            String path=uri.getPath(),name=path.substring(path.lastIndexOf('/')+1);
            int dot=name.lastIndexOf('.');
            if(dot>0&&name.substring(dot+1).toLowerCase(Locale.ROOT).matches("mp3|flac|m4a|mp4|aac|alac|ogg|opus|wav|wma|aiff|webm"))
                return name.substring(0,dot);
        }catch(IllegalArgumentException ignored){}
        return "";
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
