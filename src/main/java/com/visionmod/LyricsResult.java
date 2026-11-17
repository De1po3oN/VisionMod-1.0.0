package com.visionmod;

/** Actual words and their provenance. Plain words never acquire invented timestamps. */
public record LyricsResult(String timed,String plain,String source) {
    public static final LyricsResult EMPTY=new LyricsResult("","","");
    public LyricsResult {
        timed=timed==null?"":timed;plain=plain==null?"":plain;source=source==null?"":source;
        if(LrcTimeline.parse(timed).isEmpty())timed="";
        if(plain.length()>200_000)plain="";
        if(plain.isBlank()&&!timed.isBlank())plain=timed.replaceAll("(?m)^(?:\\[[^\\]\\r\\n]*\\])+","");
        plain=plain.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]|§","").strip();
    }
    public boolean hasText(){return !timed.isBlank()||!plain.isBlank();}
    public java.util.List<String> plainLines(){
        var lines=new java.util.ArrayList<String>();
        for(String row:plain.split("\\R")){
            row=row.strip();
            while(!row.isBlank()&&lines.size()<1200){
                int end=Math.min(50,row.length());
                if(end<row.length()){int space=row.lastIndexOf(' ',end);if(space>20)end=space;}
                lines.add(row.substring(0,end).strip());row=row.substring(end).strip();
            }
            if(lines.size()==1200)break;
        }
        return java.util.List.copyOf(lines);
    }
}
