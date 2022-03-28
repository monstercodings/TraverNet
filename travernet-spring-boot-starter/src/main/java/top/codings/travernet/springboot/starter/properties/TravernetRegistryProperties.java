package top.codings.travernet.springboot.starter.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.codings.travernet.common.bean.CryptoType;

@Getter
@Setter
@ConfigurationProperties(prefix = "travernet.registry")
public class TravernetRegistryProperties {
    /**
     * 注册中心地址
     */
    private String host;
    /**
     * 注册中心端口
     */
    private int port;
    /**
     * 连接注册中心最大重试次数
     */
    private int retryMaxCount = 3;
    /**
     * 重连失败时的基础间隔时间(毫秒)
     */
    private long retryIntervalMs = 3000;
    /**
     * 向注册中心或集群发送心跳的间隔时间(单位是秒)
     */
    private int heartbeatSecond = 30;
    /**
     * 注册中心管理的客户端超时时间(单位是秒)
     */
    private int clientTimeoutSecond = 180;
    /**
     * 传输加密类型
     */
    private CryptoType cryptoType;
    /**
     * 采用密钥加密模式时的密钥值
     */
    private String password;
    /**
     * 采用SSL加密模式并且节点作为服务端时，SSL证书路径
     */
    private String serverSslCertPath;
    /**
     * 采用SSL加密模式并且节点作为服务端时，SSL证书密码
     */
    private String serverSslCertPassword;
    /**
     * 采用SSL加密模式时，SSL协议版本
     */
    private String sslProtocol = "TLSv1.3";
    /**
     * 采用SSL加密模式并且节点作为服务端时，是否开启双向认证
     */
    private boolean needClientAuth = true;
    /**
     * 采用SSL加密模式并且节点作为客户端(或需要连接注册中心集群)时，SSL证书路径
     */
    private String clientSslCertPath;
    /**
     * 采用SSL加密模式并且节点作为客户端(或需要连接注册中心集群)时，SSL证书密码
     */
    private String clientSslCertPassword;
    /**
     * 注册中心集群所有节点的地址<br/>
     * 格式为192.168.1.1:1080<br/>
     * 集群地址可以包含自己，节点会自动识别
     */
    private String[] cluster = new String[0];
}
