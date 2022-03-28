package top.codings.travernet.client.handle;

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.model.node.bean.CloseCommand;
import top.codings.travernet.transport.bean.NodeContext;
import top.codings.travernet.transport.handle.NodeHandler;
import top.codings.travernet.transport.protocol.Message;

@Slf4j
@Getter
public class CloseCommandHandler implements NodeHandler<CloseCommand> {
    private volatile boolean forcedClose;

    @Override
    public void handle(Message<CloseCommand> message, ChannelHandlerContext ctx, NodeContext nodeContext) {
        forcedClose = true;
        log.warn("[TraverNet] 服务端要求关闭连接({})", message.getContent().getReason());
    }

    public void reset() {
        forcedClose = false;
    }
}
