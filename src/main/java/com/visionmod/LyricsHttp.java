package com.visionmod;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/** Bounded, cancellable reads. Redirects are intentionally not automatic. */
final class LyricsHttp {
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    static final class Flight {
        private final ArrayList<CompletableFuture<?>> requests=new ArrayList<>();
        private boolean cancelled;
        synchronized <T> CompletableFuture<T> watch(CompletableFuture<T> request){if(cancelled)request.cancel(true);else requests.add(request);return request;}
        synchronized boolean cancelled(){return cancelled;}
        synchronized void cancel(){cancelled=true;requests.forEach(f->f.cancel(true));requests.clear();}
    }
    record Reply(int code,String text){}
    static CompletableFuture<Reply> get(URI uri,String token,Flight flight){
        if(flight.cancelled())return CompletableFuture.failedFuture(new java.util.concurrent.CancellationException());
        var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(4))
            .header("User-Agent","VisionMod/2.0.0 (https://github.com/De1po3oN/VisionMod-1.0.0)");
        if(token!=null&&!token.isBlank())request.header("Authorization","OAuth "+token);
        var raw=flight.watch(HTTP.sendAsync(request.GET().build(),info->new BoundedLyricsBody()));
        return raw.thenApply(reply->new Reply(reply.statusCode(),new String(reply.body(),StandardCharsets.UTF_8)));
    }
    static String encode(String text){return java.net.URLEncoder.encode(text,StandardCharsets.UTF_8);}
}
