package com.visionmod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Cancel oversized HTTP bodies before buffering them. */
public final class BoundedLyricsBody implements HttpResponse.BodySubscriber<byte[]> {
    private static final int LIMIT=200_000;
    private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
    private final CompletableFuture<byte[]> result=new CompletableFuture<>();
    private Flow.Subscription subscription;
    @Override public CompletionStage<byte[]> getBody(){return result;}
    @Override public void onSubscribe(Flow.Subscription s){subscription=s;s.request(1);}
    @Override public void onNext(List<ByteBuffer> buffers){
        for(ByteBuffer b:buffers){
            if(b.remaining()>LIMIT-bytes.size()){
                subscription.cancel();result.completeExceptionally(new IOException("Lyrics body too large"));return;
            }
            byte[] chunk=new byte[b.remaining()];b.get(chunk);bytes.writeBytes(chunk);
        }
        subscription.request(1);
    }
    @Override public void onError(Throwable e){result.completeExceptionally(e);}
    @Override public void onComplete(){result.complete(bytes.toByteArray());}
}
