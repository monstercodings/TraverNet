package top.codings.travernet.common.delay;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.common.bean.DelayFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class BasicDelayManager implements DelayManager {
    private final DelayQueue<DelayFuture> queue = new DelayQueue();
    private final ExecutorService executorService = ExecutorBuilder.create()
            .setCorePoolSize(Runtime.getRuntime().availableProcessors())
            .setMaxPoolSize(Runtime.getRuntime().availableProcessors())
            .setWorkQueue(new LinkedBlockingQueue<>(Integer.MAX_VALUE))
            .setThreadFactory(ThreadUtil.newNamedThreadFactory("延迟任务线程-", false))
            .build();

    public BasicDelayManager() {
        ThreadUtil.execAsync(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DelayFuture delayFuture = queue.take();
                    executorService.submit(() -> {
                        delayFuture.getCompletableFuture().complete(delayFuture.getData());
                    });
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    log.error("执行延迟任务失败", e);
                }

            }
        }, true);
    }

    public <T> CompletableFuture<T> delay(DelayFuture<T> delayFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        delayFuture.setCompletableFuture(completableFuture);
        queue.offer(delayFuture);
        return completableFuture;
    }
}
