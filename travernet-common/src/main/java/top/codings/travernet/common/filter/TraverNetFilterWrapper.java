package top.codings.travernet.common.filter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.common.bean.BasicInvokeResult;
import top.codings.travernet.common.bean.Invocation;
import top.codings.travernet.common.bean.InvokeResult;
import top.codings.travernet.common.bean.Invoker;
import top.codings.travernet.common.error.ProviderExecuteException;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class TraverNetFilterWrapper implements TraverNetOrderFilter {
    private final TraverNetFilter source;
    @Getter
    private TraverNetFilterWrapper next;
    private final Integer reOrder;

    public TraverNetFilterWrapper(TraverNetFilter source) {
        this(source, null);
    }

    public TraverNetFilterWrapper(TraverNetFilter source, Integer order) {
        this.source = source;
        this.reOrder = order;
    }

    @Override
    public CompletableFuture<InvokeResult> invoke(Invoker invoker, Invocation invocation) {
        try {
            return source.invoke(() -> {
                if (null != next) {
                    return next.invoke(invoker, invocation);
                } else {
                    return invoker.invoke();
                }
            }, invocation);
        } catch (Exception e) {
//            log.error("[TraverNet] 过滤器执行失败", e);
            return CompletableFuture.completedFuture(BasicInvokeResult.fail(new ProviderExecuteException(e), invocation.getAttachments()));
        }
    }

    public TraverNetFilterWrapper setNext(TraverNetFilterWrapper filter) {
        next = filter;
        return next;
    }

    @Override
    public int order() {
        if (reOrder != null) {
            return reOrder.intValue();
        } else if (source instanceof TraverNetOrderFilter) {
            return ((TraverNetOrderFilter) source).order();
        } else {
            return 0;
        }
    }
}
