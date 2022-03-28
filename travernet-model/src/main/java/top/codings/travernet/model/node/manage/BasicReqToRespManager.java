package top.codings.travernet.model.node.manage;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.model.node.bean.NodeSerial;
import top.codings.travernet.model.node.bean.NodeSerialBinder;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeoutException;

@Slf4j
public class BasicReqToRespManager implements ReqToRespManager {
    private final Map<String, NodeSerialBinder> binderMap = new ConcurrentHashMap<>();
    private final DelayQueue<NodeSerialBinder> queue = new DelayQueue<>();

    public BasicReqToRespManager() {
        ThreadUtil.execAsync(() -> {
            while (true) {
                try {
                    NodeSerialBinder binder = queue.take();
                    if (!binder.canHandle()) {
                        continue;
                    }
                    binder.getCompletableFuture().completeExceptionally(new TimeoutException("请求超时"));
                    binderMap.remove(binder.getRequest().getSerialNo());
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, true);
    }

    @Override
    public CompletableFuture<Object> cache(NodeSerial request, Duration timeout) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        NodeSerialBinder binder = new NodeSerialBinder(request, future, timeout);
        queue.offer(binder);
        binderMap.put(request.getSerialNo(), binder);
        return future;
    }

    @Override
    public CompletableFuture response(String serialNo) {
        NodeSerialBinder binder = binderMap.remove(serialNo);
        if (binder == null) {
            return null;
        }
        if (binder.canHandle()) {
            return binder.getCompletableFuture();
        }
        return null;
    }
}
