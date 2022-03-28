package top.codings.travernet.registry.core;

import io.netty.channel.Channel;
import top.codings.travernet.client.core.NodeClient;
import top.codings.travernet.model.node.bean.TravernetNode;
import top.codings.travernet.transport.bean.NodeContext;

import java.util.Collection;

public interface NodeRegistry extends TravernetNode<Channel, Void>, NodeContext {
    Collection<NodeClient> getClusterClients();
}
