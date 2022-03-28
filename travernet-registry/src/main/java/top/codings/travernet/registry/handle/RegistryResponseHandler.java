package top.codings.travernet.registry.handle;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.model.node.bean.NodeResponse;
import top.codings.travernet.model.node.manage.ReqToRespManager;
import top.codings.travernet.transport.bean.NodeContext;
import top.codings.travernet.transport.handle.NodeHandler;
import top.codings.travernet.transport.protocol.Message;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class RegistryResponseHandler implements NodeHandler<NodeResponse> {
    private final ReqToRespManager reqToRespManager;

    public RegistryResponseHandler(ReqToRespManager reqToRespManager) {
        this.reqToRespManager = reqToRespManager;
    }

    @Override
    public void handle(Message<NodeResponse> message, ChannelHandlerContext ctx, NodeContext nodeContext) {
        NodeResponse content = message.getContent();
        CompletableFuture<NodeResponse> response = reqToRespManager.response(content.getSerialNo());
        if (response == null) {
            return;
        }
        response.complete(content);
    }
}
