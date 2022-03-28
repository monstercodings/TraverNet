package top.codings.travernet.common.filter.support;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.common.bean.Invocation;
import top.codings.travernet.common.bean.InvokeResult;
import top.codings.travernet.common.bean.Invoker;
import top.codings.travernet.common.filter.TraverNetOrderFilter;
import top.codings.travernet.model.node.bean.NodeType;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 执行时间开销统计过滤器
 */
@Slf4j
public class TimeExpenditureFilter implements TraverNetOrderFilter {
    /**
     * 业务执行耗时
     */
    private final static String BUSSINESS_INTERVAL_MS_KEY = "bussinessIntervalMs";
    /**
     * 网络传输耗时
     */
    private final static String NETWORK_INTERVAL_MS_KEY = "networkIntervalMs";

    @Override
    public CompletableFuture<InvokeResult> invoke(Invoker invoker, Invocation invocation) {
        NodeType nodeType = invocation.getNodeType();
        TimeInterval watch = new TimeInterval();
        return invoker.invoke().whenComplete((invokeResult, throwable) -> {
            Map<String, Object> attachments = invokeResult.getAttachments();
            long intervalMs = watch.intervalMs();
            String s = DateUtil.formatBetween(intervalMs);
            if (nodeType == NodeType.CONSUMER) {
                Object ms = attachments.get(BUSSINESS_INTERVAL_MS_KEY);
                String networkTime = "暂无记录";
                if (ms != null) {
                    long networkIntervalMs = intervalMs - Long.valueOf(ms.toString());
                    attachments.put(NETWORK_INTERVAL_MS_KEY, networkIntervalMs);
                    networkTime = DateUtil.formatBetween(networkIntervalMs);
                } else {
                    attachments.put(NETWORK_INTERVAL_MS_KEY, 0);
                    attachments.put(BUSSINESS_INTERVAL_MS_KEY, intervalMs);
                }
                if (log.isTraceEnabled()) {
                    log.trace("[TraverNet] RPC执行耗时 -> {},其中网络传输耗时 -> {}", s, networkTime);
                }
            } else if (nodeType == NodeType.PROVIDER) {
                attachments.put(BUSSINESS_INTERVAL_MS_KEY, intervalMs);
                if (log.isTraceEnabled()) {
                    log.trace("[TraverNet] 业务对象执行方法耗时 -> {}", s);
                }
            }
        });
    }

    @Override
    public int order() {
        return -1;
    }
}
