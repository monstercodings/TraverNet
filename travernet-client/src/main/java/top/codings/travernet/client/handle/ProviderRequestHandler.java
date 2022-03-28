package top.codings.travernet.client.handle;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.common.factory.NodeBeanFactory;
import top.codings.travernet.model.node.bean.NodeRequest;
import top.codings.travernet.model.node.bean.NodeResponse;
import top.codings.travernet.transport.bean.NodeContext;
import top.codings.travernet.transport.handle.NodeHandler;
import top.codings.travernet.transport.protocol.Message;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ProviderRequestHandler implements NodeHandler<NodeRequest> {
    private final NodeBeanFactory nodeBeanFactory;

    public ProviderRequestHandler(NodeBeanFactory nodeBeanFactory) {
        this.nodeBeanFactory = nodeBeanFactory;
    }

    @Override
    public void handle(Message<NodeRequest> message, ChannelHandlerContext ctx, NodeContext nodeContext) {
        NodeRequest request = message.getContent();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Class[] argTypes = Arrays.stream(request.getArgTypes()).map(s -> {
            try {
                return Class.forName(s);
            } catch (ClassNotFoundException e) {
                error.set(e);
            }
            return null;
        }).toArray(Class[]::new);
        if (error.get() != null) {
            ctx.writeAndFlush(nodeContext.getMessageCreator().transferToResponse(message, new NodeResponse(request.getSerialNo(), error.get())));
            return;
        }
        nodeBeanFactory.execute(request.getServiceName(), request.getMethod(), argTypes, request.getArgs(), message.getAttachments()).whenComplete((o, throwable) -> {
            if (throwable != null) {
                ctx.writeAndFlush(nodeContext.getMessageCreator().transferToResponse(message, new NodeResponse(request.getSerialNo(), throwable)));
                return;
            }
            ctx.writeAndFlush(nodeContext.getMessageCreator().transferToResponse(message, new NodeResponse(request.getSerialNo(), o)));
        });
    }
}
