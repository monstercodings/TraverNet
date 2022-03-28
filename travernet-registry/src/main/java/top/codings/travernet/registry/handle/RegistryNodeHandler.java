package top.codings.travernet.registry.handle;

import cn.hutool.core.util.StrUtil;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.client.core.NodeClient;
import top.codings.travernet.model.node.bean.BasicNode;
import top.codings.travernet.model.node.bean.NetNode;
import top.codings.travernet.model.node.bean.RegistryNode;
import top.codings.travernet.transport.bean.NodeContext;
import top.codings.travernet.transport.handle.NodeHandler;
import top.codings.travernet.transport.protocol.Message;

@Slf4j
public class RegistryNodeHandler implements NodeHandler<RegistryNode> {
    private final String registryId;

    public RegistryNodeHandler(String registryId) {
        this.registryId = registryId;
    }

    @Override
    public void handle(Message<RegistryNode> message, ChannelHandlerContext ctx, NodeContext context) {
        RegistryNode node = message.getContent();
        if (StrUtil.equals(node.getId(), registryId)) {
            ((NodeClient) context).close();
            return;
        }
        // TODO 更新其他注册中心信息
        if (context instanceof NodeClient) {
            NetNode target = ((NodeClient<?>) context).getTarget();
            if (target instanceof BasicNode) {
                BasicNode basicNode = (BasicNode) target;
                basicNode.setId(node.getId());
                basicNode.setNodeType(node.getNodeType());
                basicNode.setPublicNetwork(node.isPublicNetwork());
            }
        }
//        netNode.setNodeType(remote.getNodeType());
//        netNode.setPublicNetwork(remote.isPublicNetwork());
//        netNode.setId(remote.getId());
        log.info("注册中心({})信息已更新", node.getId());
    }
}
