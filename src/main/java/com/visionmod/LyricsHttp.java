package com.visionmod;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Bounded, cancellable reads. Redirects are intentionally not automatic. */
final class LyricsHttp {
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    static final class Flight {
        private final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(14);
        private final ArrayList<CompletableFuture<?>> requests=new ArrayList<>();
        private boolean cancelled;
        synchronized <T> CompletableFuture<T> watch(CompletableFuture<T> request){if(cancelled)request.cancel(true);else requests.add(request);return request;}
        synchronized boolean cancelled(){return cancelled;}
        long remainingNanos(){return deadline-System.nanoTime();}
        synchronized void cancel(){cancelled=true;requests.forEach(f->f.cancel(true));requests.clear();}
    }
    record Reply(int code,String text){}
    static CompletableFuture<Reply> get(URI uri,String token,Flight flight){
        if(flight.cancelled())return CompletableFuture.failedFuture(new java.util.concurrent.CancellationException());
        long timeout=Math.min(TimeUnit.SECONDS.toNanos(4),flight.remainingNanos());
        if(timeout<=0)return CompletableFuture.failedFuture(new TimeoutException("Lyrics lookup deadline"));
        var request=HttpRequest.newBuilder(uri).timeout(Duration.ofNanos(timeout))
            .header("User-Agent","VisionMod/2.0.0 (https://github.com/De1po3oN/VisionMod-1.0.0)");
        if(token!=null&&!token.isBlank())request.header("Authorization","OAuth "+token);
        var raw=flight.watch(HTTP.sendAsync(request.GET().build(),info->new BoundedLyricsBody()));
        // Time out the complete body as well as the initial response headers.
        return raw.copy().orTimeout(timeout,TimeUnit.NANOSECONDS)
            .whenComplete((reply,error)->{if(error instanceof TimeoutException)raw.cancel(true);})
            .thenApply(reply->new Reply(reply.statusCode(),new String(reply.body(),StandardCharsets.UTF_8)));
    }
    static String encode(String text){return java.net.URLEncoder.encode(text,StandardCharsets.UTF_8);}
}
