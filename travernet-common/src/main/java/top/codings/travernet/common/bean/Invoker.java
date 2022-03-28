package top.codings.travernet.common.bean;

import java.util.concurrent.CompletableFuture;

public interface Invoker {
    CompletableFuture<InvokeResult> invoke();
}
