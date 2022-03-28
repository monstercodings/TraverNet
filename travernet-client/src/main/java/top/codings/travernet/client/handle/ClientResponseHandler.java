package top.codings.travernet.client.handle;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadUtil;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.model.node.bean.NodeResponse;
import top.codings.travernet.model.node.manage.ReqToRespManager;
import top.codings.travernet.transport.bean.NodeContext;
import top.codings.travernet.transport.handle.NodeHandler;
import top.codings.travernet.transport.protocol.Message;
import top.codings.travernet.transport.utils.MessageUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class ClientResponseHandler implements NodeHandler<NodeResponse> {
    private final ReqToRespManager reqToRespManager;
    private final ExecutorService executorService = ExecutorBuilder.create()
            .setCorePoolSize(Runtime.getRuntime().availableProcessors())
            .setMaxPoolSize(Runtime.getRuntime().availableProcessors())
            .setWorkQueue(new LinkedBlockingQueue<>(Integer.MAX_VALUE))
            .setThreadFactory(ThreadUtil.newNamedThreadFactory("RPC异步响应-", true))
            .build();

    public ClientResponseHandler(ReqToRespManager reqToRespManager) {
        this.reqToRespManager = reqToRespManager;
    }

    @Override
    public void handle(Message<NodeResponse> message, ChannelHandlerContext ctx, NodeContext nodeContext) {
        NodeResponse response = message.getContent();
        CompletableFuture<Object> future = reqToRespManager.response(response.getSerialNo());
        if (response.isSuccess()) {
            if (future != null) {
                executorService.submit(() -> {
                    try {
                        MessageUtil.set(message);
                        future.complete(response.getData());
                    } finally {
                        MessageUtil.clean();
                    }
                });
            }
        } else {
            if (future != null) {
                executorService.submit(() -> {
                    try {
                        MessageUtil.set(message);
                        future.completeExceptionally(response.cause());
                    } finally {
                        MessageUtil.clean();
                    }
                });
            }
        }
    }
}
