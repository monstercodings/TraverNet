package top.codings.travernet.client.core;

import io.netty.channel.Channel;
import top.codings.travernet.common.factory.NodeBeanFactory;
import top.codings.travernet.model.node.bean.NetNode;
import top.codings.travernet.model.node.bean.TravernetNode;
import top.codings.travernet.transport.bean.NodeContext;
import top.codings.travernet.transport.protocol.Message;

import java.util.concurrent.CompletableFuture;

public interface NodeClient<T> extends TravernetNode<Channel, Void>, NodeContext {
    /**
     * 获取目标节点信息
     *
     * @return
     */
    NetNode getTarget();

    /**
     * 发送消息
     *
     * @param message
     * @return
     */
    CompletableFuture<T> send(Message message);

    /**
     * 获取服务对象管理工厂
     *
     * @return
     */
    NodeBeanFactory getNodeBeanFactory();
}
