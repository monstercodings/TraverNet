package top.codings.travernet.model.node.bean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BasicNode implements NetNode {
    /**
     * 节点id
     */
    private String id;
    /**
     * 节点名称
     */
    private String name;
    /**
     * 节点类型
     */
    private NodeType nodeType;
    /**
     * 是否处于公网环境
     */
    private boolean publicNetwork;
    /**
     * 服务地址
     */
    private String host;
    /**
     * 服务暴露的端口
     */
    private int port;

    public BasicNode(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public BasicNode(String id, String name, NodeType nodeType) {
        this.id = id;
        this.name = name;
        this.nodeType = nodeType;
    }
}
