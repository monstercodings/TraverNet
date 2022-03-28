package top.codings.travernet.common.filter;

import top.codings.travernet.common.bean.Invocation;
import top.codings.travernet.common.bean.InvokeResult;
import top.codings.travernet.common.bean.Invoker;

import java.util.concurrent.CompletableFuture;

public interface TraverNetFilter {
    CompletableFuture<InvokeResult> invoke(Invoker invoker, Invocation invocation);
}
