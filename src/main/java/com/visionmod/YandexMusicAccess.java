package com.visionmod;

/** User-supplied OAuth stays in this process. It is not written to configs or profiles. */
public final class YandexMusicAccess {
    private static volatile String token="",status="Плеер читается автоматически · доступ к текстам не подключён";
    private YandexMusicAccess(){}
    static String token(){return token;}
    public static boolean connected(){return !token.isBlank();}
    public static String status(){return status;}
    public static boolean connect(String value){
        value=value==null?"":value.strip().replaceFirst("(?i)^OAuth\\s+","");
        if(!value.matches("[A-Za-z0-9._~\\-]{16,2048}")){status="Нужен OAuth-токен Яндекс Музыки, а не пароль";return false;}
        token=value;status="Токен подключён на этот запуск · проверим при поиске текста";VisionLyricsLookup.clearCache();return true;
    }
    public static void disconnect(){token="";status="Доступ к текстам Яндекса отключён";VisionLyricsLookup.clearCache();}
    static void report(String expected,String message){if(token.equals(expected))status=message;}
}
