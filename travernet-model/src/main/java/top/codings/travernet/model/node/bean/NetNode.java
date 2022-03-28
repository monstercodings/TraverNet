package top.codings.travernet.model.node.bean;

import java.io.Serializable;

public interface NetNode extends Serializable {
    String getId();

    String getName();

    NodeType getNodeType();

    boolean isPublicNetwork();

    String getHost();

    int getPort();
}
