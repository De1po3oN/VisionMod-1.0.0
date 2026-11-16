package com.visionmod;

import com.google.gson.*;
import java.text.Normalizer;
import java.util.Locale;

/** Match timed lyrics to the actual song/version, including sparse browser metadata. */
public final class LyricsSearch {
    public record Query(String artist,String title){}
    private LyricsSearch(){}
    public static Query query(String artist,String title){
        artist=artist==null?"":artist.strip();title=title==null?"":title.strip();
        if(artist.isBlank()){
            int split=title.indexOf(" - ");
            if(split>0){artist=title.substring(0,split).strip();title=title.substring(split+3).strip();}
        }
        return new Query(clean(artist),clean(title));
    }
    private static String clean(String text){
        return text.replaceAll("(?i)\\s*[\\[(](?:official(?: music)? (?:video|audio)|lyrics?(?: video)?|audio|visuali[sz]er|\\d{4} remaster(?:ed)?|remaster(?:ed)?(?: \\d{4})?)[\\])]","").strip();
    }
    private static String normalized(String text){
        return Normalizer.normalize(clean(text),Normalizer.Form.NFKD).replaceAll("\\p{M}","")
            .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]","");
    }
    private static String field(JsonObject object,String key){
        return object.has(key)&&object.get(key).isJsonPrimitive()?object.get(key).getAsString():"";
    }
    public static String select(String json,Query query,long durationMs){
        JsonElement root=JsonParser.parseString(json);
        JsonArray candidates;
        if(root.isJsonArray())candidates=root.getAsJsonArray();
        else{candidates=new JsonArray();if(root.isJsonObject())candidates.add(root);}
        String wantedTitle=normalized(query.title),wantedArtist=normalized(query.artist),best="";
        double bestScore=Double.POSITIVE_INFINITY;
        for(int i=0;i<Math.min(candidates.size(),80);i++){
            if(!candidates.get(i).isJsonObject())continue;
            JsonObject song=candidates.get(i).getAsJsonObject();
            if(!normalized(field(song,"trackName")).equals(wantedTitle))continue;
            String artist=normalized(field(song,"artistName"));
            if(!wantedArtist.isBlank()&&!artist.equals(wantedArtist)&&!(artist.length()>2&&wantedArtist.contains(artist))&&!(wantedArtist.length()>2&&artist.contains(wantedArtist)))continue;
            String text=field(song,"syncedLyrics");
            if(LrcTimeline.parse(text).isEmpty())continue;
            double difference=0;
            if(durationMs>0){
                if(!song.has("duration")||song.get("duration").isJsonNull())continue;
                difference=Math.abs(song.get("duration").getAsDouble()-durationMs/1000.0);
                if(!Double.isFinite(difference)||difference>5)continue;
            }
            if(difference<bestScore){best=text;bestScore=difference;}
        }
        return best;
    }
}
