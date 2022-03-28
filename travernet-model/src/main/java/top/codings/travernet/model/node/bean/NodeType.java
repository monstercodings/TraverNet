package top.codings.travernet.model.node.bean;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public enum NodeType implements Serializable {
    /**
     * 注册节点
     */
    REGISTRY("注册节点"),
    /**
     * 服务提供者
     */
    PROVIDER("服务提供者"),
    /**
     * 服务消费者
     */
    CONSUMER("服务消费者"),
    ;
    private String desc;
}
