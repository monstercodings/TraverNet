package top.codings.travernet.springboot.starter.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.codings.travernet.model.node.bean.NodeType;
import top.codings.travernet.transport.bean.DefaultSerializationType;
import top.codings.travernet.transport.bean.SerializationType;

@Getter
@Setter
@ConfigurationProperties(prefix = "travernet.local.node")
public class TravernetLocalNodeProperties {
    /**
     * 本地节点名称
     */
    private String name;
    /**
     * 本地网络地址
     * 目前暂不支持点对点直连模式，所以该属性对除注册节点以外的节点无效
     */
    private String host;
    /**
     * 本地服务监听地址
     * 目前暂不支持点对点直连模式，所以该属性对除注册节点以外的节点无效
     */
    private int port;
    /**
     * 是否公网IP，即是否可以直接连接访问
     */
    private boolean publicNetwork;
    /**
     * 节点类型
     */
    private NodeType type;
    /**
     * 请求发起超时设置(单位是秒)
     */
    private int requestTimeoutSecond;
    /**
     * 是否启用数据压缩
     */
    private boolean compress = true;
    /**
     * 默认使用的序列化器
     */
    private DefaultSerializationType defaultSerializationType = DefaultSerializationType.HESSIAN;
}
