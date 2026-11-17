package com.visionmod;

import com.google.gson.*;
import java.text.Normalizer;
import java.util.Locale;

/** Match timed lyrics to the actual song/version, including sparse browser metadata. */
public final class LyricsSearch {
    public record Query(String artist,String title){}
    private LyricsSearch(){}
    public static Query query(String artist,String title){
        artist=cleanArtist(artist==null?"":artist.strip());title=clean(title==null?"":title.strip());
        String[] parts=title.split("\\s+(?:-|—|–|\\|)\\s+",2);
        if(parts.length==2&&!parts[0].isBlank()&&!parts[1].isBlank()){
            if(artist.isBlank()){artist=cleanArtist(parts[0]);title=parts[1].strip();}
            else if(normalized(cleanArtist(parts[0])).equals(normalized(artist)))title=parts[1].strip();
        }
        return new Query(clean(artist),clean(title));
    }
    private static String cleanArtist(String text){
        return clean(text).replaceAll("(?iu)\\s*[-—–]\\s*(?:topic|official)\\s*$","")
            .replaceAll("(?i)VEVO$","").strip();
    }
    private static String clean(String text){
        return text.replaceAll("(?iu)\\s*(?:[—–|·]| - )\\s*(?:яндекс[ .]?музыка|yandex\\s*music)\\s*$","")
            .replaceAll("(?iu)\\s*[\\[(](?:official(?: music)?(?: (?:video|audio))?(?: (?:hd|4k))?|lyrics?(?: video)?|audio|visuali[sz]er|текст(?: песни)?|официальн(?:ое|ый) (?:видео|клип)|\\d{4} remaster(?:ed)?|remaster(?:ed)?(?: \\d{4})?)[\\])]","")
            .replaceAll("\\s+"," ").strip();
    }
    private static String normalized(String text){
        return Normalizer.normalize(clean(text),Normalizer.Form.NFKD).replaceAll("\\p{M}","")
            .toLowerCase(Locale.ROOT).replace('ё','е').replaceAll("[^\\p{L}\\p{N}]","");
    }
    private static String field(JsonObject object,String key){
        return object.has(key)&&object.get(key).isJsonPrimitive()?object.get(key).getAsString():"";
    }
    public static String select(String json,Query query,long durationMs){
        return selectResult(json,query,durationMs,"LRCLIB").timed();
    }
    public static boolean matches(String title,String artist,Query query){
        if(!normalized(title).equals(normalized(query.title)))return false;
        String actual=normalized(cleanArtist(artist)),wanted=normalized(query.artist);
        if(wanted.isBlank()||actual.equals(wanted))return true;
        String a=primaryArtist(artist),b=primaryArtist(query.artist);
        return a.length()>2&&a.equals(b);
    }
    private static String primaryArtist(String value){return normalized(cleanArtist(value).split("(?iu)(?:\\s+(?:feat\\.?|ft\\.?|featuring|&|x)\\s+|\\s*,\\s*)",2)[0]);}
    public static LyricsResult selectResult(String json,Query query,long durationMs,String provider){
        JsonElement root=JsonParser.parseString(json);
        JsonArray candidates;
        if(root.isJsonArray())candidates=root.getAsJsonArray();
        else{candidates=new JsonArray();if(root.isJsonObject())candidates.add(root);}
        LyricsResult best=LyricsResult.EMPTY;
        double bestScore=Double.POSITIVE_INFINITY;
        for(int i=0;i<Math.min(candidates.size(),80);i++){
            if(!candidates.get(i).isJsonObject())continue;
            JsonObject song=candidates.get(i).getAsJsonObject();
            if(!matches(field(song,"trackName"),field(song,"artistName"),query))continue;
            var text=new LyricsResult(field(song,"syncedLyrics"),field(song,"plainLyrics"),provider);
            if(!text.hasText())continue;
            double difference=0;
            if(durationMs>0){
                if(!song.has("duration")||song.get("duration").isJsonNull())continue;
                try{difference=Math.abs(song.get("duration").getAsDouble()-durationMs/1000.0);}catch(RuntimeException ignored){continue;}
                if(!Double.isFinite(difference)||difference>5)continue;
            }
            double score=difference+(text.timed().isBlank()?100:0);
            if(score<bestScore){best=text;bestScore=score;}
        }
        return best;
    }
}
