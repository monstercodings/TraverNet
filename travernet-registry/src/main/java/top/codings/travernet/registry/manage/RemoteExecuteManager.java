package top.codings.travernet.registry.manage;

import top.codings.travernet.model.node.bean.NodeRequest;
import top.codings.travernet.registry.bean.NetNodeRecord;
import top.codings.travernet.transport.protocol.Message;

import java.util.concurrent.CompletableFuture;

public interface RemoteExecuteManager {
    CompletableFuture<NetNodeRecord> execute(Message<NodeRequest> message);
}
