package top.codings.travernet.common.delay;

import top.codings.travernet.common.bean.DelayFuture;

import java.util.concurrent.CompletableFuture;

public interface DelayManager {
    <T> CompletableFuture<T> delay(DelayFuture<T> delayFuture);
}
