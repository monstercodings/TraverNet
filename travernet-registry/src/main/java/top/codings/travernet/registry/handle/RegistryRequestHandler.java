package top.codings.travernet.registry.handle;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.model.node.bean.NodeRequest;
import top.codings.travernet.model.node.bean.NodeResponse;
import top.codings.travernet.model.node.manage.ReqToRespManager;
import top.codings.travernet.registry.bean.NetNodeRecord;
import top.codings.travernet.registry.manage.RemoteExecuteManager;
import top.codings.travernet.transport.bean.NodeContext;
import top.codings.travernet.transport.handle.NodeHandler;
import top.codings.travernet.transport.protocol.Message;
import top.codings.travernet.transport.utils.MessageUtil;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RegistryRequestHandler implements NodeHandler<NodeRequest> {
    private final int requestTimeoutSecond;
    private final RemoteExecuteManager remoteExecuteManager;
    private final ReqToRespManager reqToRespManager;

    public RegistryRequestHandler(int requestTimeoutSecond, RemoteExecuteManager remoteExecuteManager, ReqToRespManager reqToRespManager) {
        this.requestTimeoutSecond = requestTimeoutSecond;
        this.remoteExecuteManager = remoteExecuteManager;
        this.reqToRespManager = reqToRespManager;
    }

    @Override
    public void handle(Message<NodeRequest> message, ChannelHandlerContext ctx, NodeContext nodeContext) {
        CompletableFuture<NetNodeRecord> completableFuture = remoteExecuteManager.execute(message);
        completableFuture.whenComplete((netNodeRecord, throwable) -> {
            NodeRequest request = message.getContent();
            if (throwable != null) {
                ctx.writeAndFlush(nodeContext.getMessageCreator().transferToResponse(message, new NodeResponse(request.getSerialNo(), throwable)));
                return;
            }
            // 将请求挂起等待响应
            reqToRespManager.cache(request, Duration.ofSeconds(requestTimeoutSecond)).whenComplete((response, err) -> {
                if (err != null) {
                    ctx.writeAndFlush(nodeContext.getMessageCreator().transferToResponse(message, new NodeResponse(request.getSerialNo(), err)));
                    return;
                }
                ctx.writeAndFlush(MessageUtil.get());
            });
        });
    }
}
