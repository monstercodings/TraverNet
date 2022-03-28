package top.codings.travernet.model.node.bean;

public class ConsumerNode extends BasicNode {
    public ConsumerNode() {
        super(NodeType.CONSUMER);
    }

    public ConsumerNode(String id, String name) {
        super(id, name, NodeType.CONSUMER);
    }
}
