package top.codings.travernet.common.filter.support;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import top.codings.travernet.common.bean.Invocation;
import top.codings.travernet.common.bean.InvokeResult;
import top.codings.travernet.common.bean.Invoker;
import top.codings.travernet.common.filter.TraverNetFilter;
import top.codings.travernet.common.utils.TraverNetUtil;
import top.codings.travernet.model.node.bean.NodeType;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * <p>MDC日志链路ID过滤器</p>
 * <p>需要放在过滤器链的最后一个</p>
 */
public class MDCTraceIdFilter implements TraverNetFilter {
    private final static String TRACE_ID_KEY = "traceId";

    @Override
    public CompletableFuture<InvokeResult> invoke(Invoker invoker, Invocation invocation) {
        NodeType nodeType = invocation.getNodeType();
        if (nodeType == NodeType.PROVIDER) {
            // 生产者链路ID从消费者透传过来的附件里取
            Object traceIdObj = invocation.getAttachments().get(TRACE_ID_KEY);
            if (null != traceIdObj) {
                TraverNetUtil.setCurrentTraceId(traceIdObj.toString());
            }
        } else if (nodeType == NodeType.CONSUMER) {
            // 消费者从MDC里取链路ID透传给生产者
            String traceId = TraverNetUtil.getCurrentTraceId();
            if (StrUtil.isNotBlank(traceId)) {
                invocation.getAttachments().put(TRACE_ID_KEY, traceId);
            }
        }
        return invoker.invoke().whenComplete((invokeResult, throwable) -> {
            if (nodeType == NodeType.PROVIDER) {
                TraverNetUtil.removeCurrentTraceId();
            } else if (nodeType == NodeType.CONSUMER) {
                Map<String, Object> attachments = invokeResult.getAttachments();
                if (null != attachments) {
                    Object o = attachments.get(TRACE_ID_KEY);
                    if (ObjectUtil.isNotEmpty(o)) {
                        TraverNetUtil.setCurrentTraceId(o.toString());
                    }
                }
            }
        });
    }
}
