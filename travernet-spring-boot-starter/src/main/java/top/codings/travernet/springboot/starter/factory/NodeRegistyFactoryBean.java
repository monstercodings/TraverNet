package top.codings.travernet.springboot.starter.factory;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import top.codings.travernet.common.delay.DelayManager;
import top.codings.travernet.common.error.TraverConfigurationException;
import top.codings.travernet.model.node.bean.NodeType;
import top.codings.travernet.model.node.manage.ReqToRespManager;
import top.codings.travernet.registry.core.NodeRegistry;
import top.codings.travernet.registry.core.RegistryBuilder;
import top.codings.travernet.springboot.starter.manage.NodeIdManager;
import top.codings.travernet.springboot.starter.properties.TravernetLocalNodeProperties;
import top.codings.travernet.springboot.starter.properties.TravernetRegistryProperties;
import top.codings.travernet.transport.manage.CryptoManager;

public class NodeRegistyFactoryBean implements FactoryBean<NodeRegistry> {
    private final Log log = LogFactory.getLog(getClass());
    private final String applicationName;
    private final TravernetLocalNodeProperties travernetLocalNodeProperties;
    private final TravernetRegistryProperties travernetRegistryProperties;
    private final ReqToRespManager reqToRespManager;
    private final DelayManager delayManager;
    private final NodeIdManager nodeIdManager;
    private final CryptoManager cryptoManager;

    public NodeRegistyFactoryBean(
            @Value("${spring.application.name}") String applicationName,
            TravernetLocalNodeProperties travernetLocalNodeProperties,
            TravernetRegistryProperties travernetRegistryProperties,
            ReqToRespManager reqToRespManager,
            DelayManager delayManager,
            NodeIdManager nodeIdManager,
            CryptoManager cryptoManager) {
        this.applicationName = applicationName;
        this.travernetLocalNodeProperties = travernetLocalNodeProperties;
        this.travernetRegistryProperties = travernetRegistryProperties;
        this.reqToRespManager = reqToRespManager;
        this.delayManager = delayManager;
        this.nodeIdManager = nodeIdManager;
        this.cryptoManager = cryptoManager;
    }

    @Override
    public NodeRegistry getObject() throws Exception {
        String name = applicationName;
        if (StrUtil.isNotBlank(travernetLocalNodeProperties.getName())) {
            name = travernetLocalNodeProperties.getName();
        }
        if (StrUtil.isBlank(name)) {
            throw new TraverConfigurationException("节点名称不能为空");
        }
        NodeRegistry registry = RegistryBuilder.create(nodeIdManager.findId(NodeType.REGISTRY), name)
                .host(travernetLocalNodeProperties.getHost())
                .port(travernetLocalNodeProperties.getPort())
                .publicNetwork(travernetLocalNodeProperties.isPublicNetwork())
                .cluster(travernetRegistryProperties.getCluster())
                .clientTimeoutSecond(travernetRegistryProperties.getClientTimeoutSecond())
                .heartbeatSecond(travernetRegistryProperties.getHeartbeatSecond())
                .requestTimeoutSecond(travernetLocalNodeProperties.getRequestTimeoutSecond())
                .delayManager(delayManager)
                .reqToRespManager(reqToRespManager)
                .encryptType(travernetRegistryProperties.getCryptoType())
                .password(travernetRegistryProperties.getPassword())
                .serverSslCertPath(travernetRegistryProperties.getServerSslCertPath())
                .serverSslCertPassword(travernetRegistryProperties.getServerSslCertPassword())
                .sslProtocol(travernetRegistryProperties.getSslProtocol())
                .needClientAuth(travernetRegistryProperties.isNeedClientAuth())
                .clientSslCertPath(travernetRegistryProperties.getClientSslCertPath())
                .clientSslCertPassword(travernetRegistryProperties.getClientSslCertPassword())
                .cryptoManager(cryptoManager)
                .defaultSerializationType(travernetLocalNodeProperties.getDefaultSerializationType())
                .compress(travernetLocalNodeProperties.isCompress())
                .build();
        return registry;
    }

    @Override
    public Class<?> getObjectType() {
        return NodeRegistry.class;
    }
}
