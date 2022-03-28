package top.codings.travernet.client.core;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.client.bean.RetryMessage;
import top.codings.travernet.common.bean.DelayFuture;
import top.codings.travernet.common.delay.DelayManager;
import top.codings.travernet.common.error.ConnectFailException;
import top.codings.travernet.common.error.NoConnectException;
import top.codings.travernet.transport.protocol.Message;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

@Slf4j
public abstract class RetryNodeClient<T> implements NodeClient<T> {
    private final long retryIntervalMs;
    private final int retryMaxCount;
    @Getter
    private final DelayManager delayManager;
    private final TransferQueue<RetryMessage> waitToSendQueue = new LinkedTransferQueue<>();

    protected RetryNodeClient(long retryIntervalMs, int retryMaxCount, DelayManager delayManager) {
        this.retryIntervalMs = retryIntervalMs;
        this.retryMaxCount = retryMaxCount;
        this.delayManager = delayManager;
    }

    protected abstract CompletableFuture<Channel> doStart();

    protected abstract CompletableFuture<T> doSend(Message message);

    @Override
    public CompletableFuture<Channel> start() {
        CompletableFuture<Channel> result = new CompletableFuture();
        doStart(result, 0);
        return result;
    }

    private void doStart(CompletableFuture<Channel> result, int retry) {
        CompletableFuture<Channel> completableFuture = doStart();
        completableFuture.whenComplete((channel, throwable) -> {
            if (throwable != null) {
                int nowRetry = retry + 1;
                try {
                    verify(nowRetry).whenComplete((o, thr) -> {
                        log.info("[TraverNet] 第{}次重试连接注册中心", nowRetry);
                        doStart(result, nowRetry);
                    });
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
                return;
            }
            reSendFailMessage();
            result.complete(channel);
        });
    }

    /**
     * 重新发送尚未发送的消息
     */
    private void reSendFailMessage() {
        List<RetryMessage> list = new LinkedList<>();
        waitToSendQueue.drainTo(list);
        if (log.isDebugEnabled()) {
            log.debug("需要重新发送的消息总计{}条", list.size());
        }
        for (RetryMessage retryMessage : list) {
            doSend(retryMessage);
        }
    }

    private CompletableFuture verify(int retry) throws ConnectFailException {
        if (retryMaxCount < 0) {
        } else if (retry > retryMaxCount) {
            throw new ConnectFailException(String.format("当前重试次数(%s)已超过最大重试次数(%s)", retry, retryMaxCount));
        } else {
        }
        DelayFuture delayFuture = new DelayFuture(retryIntervalMs * retry);
        return delayManager.delay(delayFuture);
    }

    @Override
    public CompletableFuture<T> send(Message message) {
        RetryMessage retryMessage = new RetryMessage(message);
        doSend(retryMessage);
        return retryMessage.getFuture();
    }

    private void doSend(RetryMessage retryMessage) {
        doSend(retryMessage.getMessage()).whenComplete((t, throwable) -> {
            if (throwable == null) {
                retryMessage.getFuture().complete(t);
                return;
            }
            // 发送失败则将重试次数+1
            retryMessage.setLastError(throwable);
            retryMessage.setRetry(retryMessage.getRetry() + 1);
            if (retryMessage.getRetry() > retryMaxCount) {
                if (log.isTraceEnabled()) {
                    log.trace(String.format("当前重试发送消息次数(%s)已超过最大重试次数(%s)", retryMessage.getRetry(), retryMaxCount));
                }
                retryMessage.getFuture().completeExceptionally(throwable);
                return;
            }
            if (throwable instanceof NoConnectException) {
                waitToSendQueue.offer(retryMessage);
                return;
            }
            delayManager.delay(new DelayFuture(retryIntervalMs)).whenComplete((o, o2) -> {
                if (log.isTraceEnabled()) {
                    log.trace("第{}次重试发送消息({})", retryMessage.getRetry(), retryMessage.getMessage().getHeader().getMessageId());
                }
                doSend(retryMessage);
            });
        });
    }
}
