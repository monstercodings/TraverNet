package top.codings.travernet.springboot.starter.manage;

import top.codings.travernet.model.node.bean.NodeType;

public interface NodeIdManager {
    String findId(NodeType nodeType);
}
