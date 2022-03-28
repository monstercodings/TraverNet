package top.codings.travernet.transport.bean;

import lombok.Getter;
import top.codings.travernet.transport.handle.NodeHandler;

@Getter
public class NodeHandlerRecorder {
    private final Class paramClass;
    private final NodeHandler nodeHandler;
    private final int order;

    public NodeHandlerRecorder(Class paramClass, NodeHandler nodeHandler, int order) {
        this.paramClass = paramClass;
        this.nodeHandler = nodeHandler;
        this.order = order;
    }
}
