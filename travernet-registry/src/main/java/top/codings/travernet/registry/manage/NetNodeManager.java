package top.codings.travernet.registry.manage;

import io.netty.channel.Channel;
import top.codings.travernet.client.core.NodeClient;
import top.codings.travernet.model.node.bean.NetNode;

import java.util.Collection;

public interface NetNodeManager {
    void record(NetNode node, Channel channel);

    void offline(NetNode node);

    /**
     * 推送节点更新信息给集群
     *
     * @param clusterClients
     */
    void notifyClusters(String skipId, Collection<NodeClient> clusterClients);
}
