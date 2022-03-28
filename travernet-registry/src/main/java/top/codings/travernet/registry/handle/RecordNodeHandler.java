package top.codings.travernet.registry.handle;

import cn.hutool.core.util.StrUtil;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.model.node.bean.NetNode;
import top.codings.travernet.model.node.bean.NodeType;
import top.codings.travernet.registry.core.NodeRegistry;
import top.codings.travernet.registry.manage.NetNodeManager;
import top.codings.travernet.transport.bean.NodeContext;
import top.codings.travernet.transport.handle.NodeHandler;
import top.codings.travernet.transport.protocol.Message;

@Slf4j
public class RecordNodeHandler implements NodeHandler<NetNode> {
    private final NetNodeManager netNodeManager;

    public RecordNodeHandler(NetNodeManager netNodeManager) {
        this.netNodeManager = netNodeManager;
    }

    @Override
    public void handle(Message<NetNode> message, ChannelHandlerContext ctx, NodeContext nodeContext) {
        NetNode node = message.getContent();
        String id = nodeContext.getNetNode().getId();
        if (!StrUtil.equals(node.getId(), id)) {
            // 缓存节点信息
            netNodeManager.record(node, ctx.channel());
        }
        ctx.writeAndFlush(nodeContext.getMessageCreator().transferToResponse(message, nodeContext.getNetNode()));
    }
}
