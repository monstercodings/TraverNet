package top.codings.travernet.model.node.bean;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryNode extends BasicNode {
    /**
     * 服务id对应的注册中心id
     */
    @Getter
    private final Map<String, Collection<InnerNode>> serviceNameToRegistryIdsMap = new ConcurrentHashMap<>();

    public RegistryNode() {
        super(NodeType.REGISTRY);
    }

    public RegistryNode(String id, String name) {
        super(id, name, NodeType.REGISTRY);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InnerNode implements Serializable {
        private String id;
        private NodeType nodeType;
    }

}
