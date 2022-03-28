package top.codings.travernet.springboot.starter.factory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import top.codings.travernet.client.build.NodeClientBuilder;
import top.codings.travernet.client.core.NodeClient;
import top.codings.travernet.common.delay.DelayManager;
import top.codings.travernet.common.error.TraverConfigurationException;
import top.codings.travernet.common.error.TraverNetException;
import top.codings.travernet.common.factory.NodeBeanFactory;
import top.codings.travernet.common.filter.TraverNetFilter;
import top.codings.travernet.model.node.bean.*;
import top.codings.travernet.model.node.manage.ReqToRespManager;
import top.codings.travernet.springboot.starter.manage.NodeIdManager;
import top.codings.travernet.springboot.starter.properties.TravernetLocalNodeProperties;
import top.codings.travernet.springboot.starter.properties.TravernetRegistryProperties;
import top.codings.travernet.transport.manage.CryptoManager;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

public class NodeClientFactoryBean implements FactoryBean<NodeClient> {
    private final Log log = LogFactory.getLog(getClass());
    private final String applicationName;
    private final TravernetLocalNodeProperties travernetLocalNodeProperties;
    private final TravernetRegistryProperties travernetRegistryProperties;
    private final NodeBeanFactory nodeBeanFactory;
    private final ReqToRespManager reqToRespManager;
    private final DelayManager delayManager;
    private final NodeIdManager nodeIdManager;
    private final CryptoManager cryptoManager;

    public NodeClientFactoryBean(
            @Value("${spring.application.name}") String applicationName,
            TravernetLocalNodeProperties travernetLocalNodeProperties,
            TravernetRegistryProperties travernetRegistryProperties,
            NodeBeanFactory nodeBeanFactory,
            ReqToRespManager reqToRespManager,
            DelayManager delayManager,
            NodeIdManager nodeIdManager,
            CryptoManager cryptoManager,
            Collection<TraverNetFilter> filters) {
        this.applicationName = applicationName;
        this.travernetLocalNodeProperties = travernetLocalNodeProperties;
        this.travernetRegistryProperties = travernetRegistryProperties;
        this.nodeBeanFactory = nodeBeanFactory;
        this.reqToRespManager = reqToRespManager;
        this.delayManager = delayManager;
        this.nodeIdManager = nodeIdManager;
        this.cryptoManager = cryptoManager;
        if (CollUtil.isNotEmpty(filters)) {
            for (TraverNetFilter filter : filters) {
                nodeBeanFactory.addFilter(filter);
            }
        }
    }

    @Override
    public NodeClient getObject() throws Exception {
        String name = applicationName;
        if (StrUtil.isNotBlank(travernetLocalNodeProperties.getName())) {
            name = travernetLocalNodeProperties.getName();
        }
        if (StrUtil.isBlank(name)) {
            throw new TraverConfigurationException("节点名称不能为空");
        }
        NetNode netNode;
        String localHost = travernetLocalNodeProperties.getHost();
        localHost = StrUtil.isBlank(localHost) ? "localhost" : localHost;
        int localPort = travernetLocalNodeProperties.getPort();
        boolean publicNetwork = travernetLocalNodeProperties.isPublicNetwork();
        NodeType nodeType = travernetLocalNodeProperties.getType();
        switch (nodeType) {
            case PROVIDER:
                ProviderNode providerNode = new ProviderNode(nodeIdManager.findId(nodeType), name);
                providerNode.setHost(localHost);
                providerNode.setPort(localPort);
                providerNode.setPublicNetwork(publicNetwork);
                netNode = providerNode;
                break;
            case CONSUMER:
                ConsumerNode consumerNode = new ConsumerNode(nodeIdManager.findId(nodeType), name);
                consumerNode.setHost(localHost);
                consumerNode.setPort(localPort);
                consumerNode.setPublicNetwork(publicNetwork);
                netNode = consumerNode;
                break;
            case REGISTRY:
                RegistryNode registryNode = new RegistryNode(nodeIdManager.findId(nodeType), name);
                registryNode.setHost(localHost);
                registryNode.setPort(localPort);
                registryNode.setPublicNetwork(publicNetwork);
                netNode = registryNode;
                break;
            default:
                throw new IllegalAccessException("仅支持选择注册节点/生产节点/消费节点三种类型");
        }
        RegistryNode target = new RegistryNode();
        String host = travernetRegistryProperties.getHost();
        if (StrUtil.isBlank(host)) {
            throw new IllegalArgumentException("注册中心的地址(travernet.registry.host)不能为空");
        }
        target.setHost(host);
        int port = travernetRegistryProperties.getPort();
        if (port <= 0) {
            throw new IllegalArgumentException("注册中心的端口号(travernet.registry.port)必须在正常端口范围内");
        }
        target.setPort(port);
        int retryMaxCount = travernetRegistryProperties.getRetryMaxCount();
        if (retryMaxCount < 0) {
            retryMaxCount = 0;
        }
        long retryIntervalMs = travernetRegistryProperties.getRetryIntervalMs();
        if (retryIntervalMs <= 0) {
            retryIntervalMs = 3000;
        }
        NodeClient client = NodeClientBuilder.create()
                .local(netNode)
                .target(target)
                .retryIntervalMs(retryIntervalMs)
                .retryMaxCount(retryMaxCount)
                .heartbeatSecond(travernetRegistryProperties.getHeartbeatSecond())
                .delayManager(delayManager)
                .reqToRespManager(reqToRespManager)
                .encryptType(travernetRegistryProperties.getCryptoType())
                .password(travernetRegistryProperties.getPassword())
                .clientSslCertPath(travernetRegistryProperties.getClientSslCertPath())
                .clientSslCertPassword(travernetRegistryProperties.getClientSslCertPassword())
                .sslProtocol(travernetRegistryProperties.getSslProtocol())
                .requestTimeoutSecond(travernetLocalNodeProperties.getRequestTimeoutSecond())
                .nodeBeanFactory(nodeBeanFactory)
                .cryptoManager(cryptoManager)
                .defaultSerializationType(travernetLocalNodeProperties.getDefaultSerializationType())
                .compress(travernetLocalNodeProperties.isCompress())
                .build();
        log.info(String.format("[TraverNet] %s已配置完成，启动连接注册中心(%s:%s)...", netNode.getNodeType().getDesc(), target.getHost(), target.getPort()));
        try {
            client.start().get();
        } catch (Throwable e) {
            if (e instanceof ExecutionException && e.getCause() != null) {
                e = e.getCause();
            }
            throw new TraverNetException("travernet启动失败", e);
        }
        log.info(String.format("[TraverNet] %s成功连接注册中心(%s:%s)", netNode.getNodeType().getDesc(), target.getHost(), target.getPort()));
        return client;
    }

    @Override
    public Class<?> getObjectType() {
        return NodeClient.class;
    }
}
