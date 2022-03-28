package top.codings.travernet.common.filter.support;

import top.codings.travernet.common.bean.Invocation;
import top.codings.travernet.common.bean.InvokeResult;
import top.codings.travernet.common.bean.Invoker;
import top.codings.travernet.common.filter.TraverNetFilter;

import java.util.concurrent.CompletableFuture;

public final class NopFilter implements TraverNetFilter {
    @Override
    public CompletableFuture<InvokeResult> invoke(Invoker invoker, Invocation invocation) {
        return invoker.invoke();
    }
}
