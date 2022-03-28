package top.codings.travernet.transport.handle;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.transport.bean.NodeContext;
import top.codings.travernet.transport.bean.NodeHandlerRecorder;
import top.codings.travernet.transport.protocol.Message;
import top.codings.travernet.transport.utils.MessageUtil;

import java.util.Collection;

@Slf4j
public class NodeHandleWrapper extends ChannelInboundHandlerAdapter {
    private final NodeContext nodeContext;
    private final Collection<NodeHandlerRecorder> recorders;

    public NodeHandleWrapper(NodeContext nodeContext, Collection<NodeHandlerRecorder> recorders) {
        this.nodeContext = nodeContext;
        this.recorders = recorders;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            Message message = MessageUtil.get();
            recorders.stream()
                    .filter(recorder -> recorder.getParamClass().isAssignableFrom(message.getContent().getClass()))
                    .findFirst()
                    .ifPresent(recorder -> recorder.getNodeHandler().handle(message, ctx, nodeContext));
        } finally {
            MessageUtil.clean();
            ReferenceCountUtil.release(msg);
        }
    }
}
