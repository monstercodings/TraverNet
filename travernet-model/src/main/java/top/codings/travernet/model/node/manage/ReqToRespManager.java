package top.codings.travernet.model.node.manage;

import top.codings.travernet.model.node.bean.NodeSerial;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public interface ReqToRespManager {
    CompletableFuture<Object> cache(NodeSerial request, Duration timeout);

    CompletableFuture response(String serialNo);
}
