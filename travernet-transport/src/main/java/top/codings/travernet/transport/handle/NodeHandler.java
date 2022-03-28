package top.codings.travernet.transport.handle;

import io.netty.channel.ChannelHandlerContext;
import top.codings.travernet.transport.bean.NodeContext;
import top.codings.travernet.transport.protocol.Message;

import java.util.concurrent.atomic.AtomicReference;

public interface NodeHandler<T> {
    void handle(Message<T> message, ChannelHandlerContext ctx, NodeContext nodeContext);
}
