package top.codings.travernet.registry.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import top.codings.travernet.common.bean.CryptoType;
import top.codings.travernet.common.delay.BasicDelayManager;
import top.codings.travernet.common.delay.DelayManager;
import top.codings.travernet.common.error.TraverConfigurationException;
import top.codings.travernet.common.factory.BasicSSLContextFactory;
import top.codings.travernet.common.factory.SSLContextFactory;
import top.codings.travernet.model.node.bean.NetNode;
import top.codings.travernet.model.node.bean.NodeRequest;
import top.codings.travernet.model.node.bean.NodeResponse;
import top.codings.travernet.model.node.bean.RegistryNode;
import top.codings.travernet.model.node.manage.BasicReqToRespManager;
import top.codings.travernet.model.node.manage.ReqToRespManager;
import top.codings.travernet.registry.bean.RegistryConfig;
import top.codings.travernet.registry.handle.RecordNodeHandler;
import top.codings.travernet.registry.handle.RegistryRequestHandler;
import top.codings.travernet.registry.handle.RegistryResponseHandler;
import top.codings.travernet.registry.manage.BasicNetNodeManager;
import top.codings.travernet.registry.manage.NetNodeManager;
import top.codings.travernet.registry.manage.RemoteExecuteManager;
import top.codings.travernet.transport.bean.*;
import top.codings.travernet.transport.handle.NodeHandler;
import top.codings.travernet.transport.manage.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RegistryBuilder {
    private final RegistryNode local;
    private Collection<NetNode> cluster = new LinkedList<>();
    private DelayManager delayManager;
    private SerializationManager serializationManager;
    private ContentTypeManager contentTypeManager;
    private CompressManager compressManager;
    private NetNodeManager netNodeManager;
    private ReqToRespManager reqToRespManager;
    private CryptoManager cryptoManager;
    private SSLContextFactory sslContextFactory;
    private MessageCreator messageCreator;
    private boolean compress = true;
    private SerializationType serializationType;
    private final List<NodeHandlerRecorder> recorders = new CopyOnWriteArrayList<>();
    private final RegistryConfig registryConfig = new RegistryConfig();

    private RegistryBuilder(String id, String name) {
        local = new RegistryNode(id, name);
    }

    public final static RegistryBuilder create(String id, String name) {
        RegistryBuilder builder = new RegistryBuilder(id, name);
        return builder;
    }

    public RegistryBuilder host(String host) {
        local.setHost(host);
        return this;
    }

    public RegistryBuilder port(int port) {
        local.setPort(port);
        return this;
    }

    public RegistryBuilder publicNetwork(boolean isPublic) {
        local.setPublicNetwork(isPublic);
        return this;
    }

    public RegistryBuilder serializationManager(SerializationManager serializationManager) {
        this.serializationManager = serializationManager;
        return this;
    }

    public RegistryBuilder contentTypeManager(ContentTypeManager contentTypeManager) {
        this.contentTypeManager = contentTypeManager;
        return this;
    }

    public RegistryBuilder compressManager(CompressManager compressManager) {
        this.compressManager = compressManager;
        return this;
    }

    public RegistryBuilder netNodeManager(NetNodeManager netNodeManager) {
        this.netNodeManager = netNodeManager;
        return this;
    }

    public RegistryBuilder sslContextFactory(SSLContextFactory sslContextFactory) {
        this.sslContextFactory = sslContextFactory;
        return this;
    }

    public RegistryBuilder clientTimeoutSecond(int clientTimeoutSecond) {
        registryConfig.setClientTimeoutSecond(clientTimeoutSecond);
        return this;
    }

    public RegistryBuilder heartbeatSecond(int heartbeatSecond) {
        registryConfig.setHeartbeatSecond(heartbeatSecond);
        return this;
    }

    public RegistryBuilder requestTimeoutSecond(int requestTimeoutSecond) {
        registryConfig.setRequestTimeoutSecond(requestTimeoutSecond);
        return this;
    }

    public RegistryBuilder encryptType(CryptoType cryptoType) {
        registryConfig.setCryptoType(cryptoType);
        return this;
    }

    public RegistryBuilder password(String password) {
        registryConfig.setPassword(password);
        return this;
    }

    public RegistryBuilder serverSslCertPath(String serverSslCertPath) {
        registryConfig.setServerSslCertPath(serverSslCertPath);
        return this;
    }

    public RegistryBuilder serverSslCertPassword(String serverSslCertPassword) {
        registryConfig.setServerSslCertPassword(serverSslCertPassword);
        return this;
    }

    public RegistryBuilder sslProtocol(String sslProtocol) {
        registryConfig.setSslProtocol(sslProtocol);
        return this;
    }

    public RegistryBuilder needClientAuth(boolean needClientAuth) {
        registryConfig.setNeedClientAuth(needClientAuth);
        return this;
    }

    public RegistryBuilder clientSslCertPath(String clientSslCertPath) {
        registryConfig.setClientSslCertPath(clientSslCertPath);
        return this;
    }

    public RegistryBuilder clientSslCertPassword(String clientSslCertPassword) {
        registryConfig.setClientSslCertPassword(clientSslCertPassword);
        return this;
    }

    public RegistryBuilder messageCreator(MessageCreator messageCreator) {
        this.messageCreator = messageCreator;
        return this;
    }

    public RegistryBuilder compress(boolean compress) {
        this.compress = compress;
        return this;
    }

    public RegistryBuilder defaultSerializationType(SerializationType serializationType) {
        this.serializationType = serializationType;
        return this;
    }

    public RegistryBuilder cluster(String... hosts) {
        if (ObjectUtil.isNotEmpty(hosts)) {
            for (String host : hosts) {
                String[] split = host.split(":");
                String ip = split[0];
                int port = Integer.parseInt(split[1]);
                boolean exist = cluster.stream().anyMatch(n -> StrUtil.equals(n.getHost(), ip) && (port == n.getPort()));
                if (exist) {
                    continue;
                }
                RegistryNode node = new RegistryNode();
                node.setHost(ip);
                node.setPort(port);
                cluster.add(node);
            }
        }
        return this;
    }

    public <T> RegistryBuilder nodeHandler(Class<T> aClass, NodeHandler<T> nodeHandler) {
        return nodeHandler(aClass, nodeHandler, Integer.MAX_VALUE);
    }

    public <T> RegistryBuilder nodeHandler(Class<T> aClass, NodeHandler<T> nodeHandler, int order) {
        recorders.add(new NodeHandlerRecorder(aClass, nodeHandler, order));
        return this;
    }

    public RegistryBuilder delayManager(DelayManager delayManager) {
        this.delayManager = delayManager;
        return this;
    }

    public RegistryBuilder reqToRespManager(ReqToRespManager reqToRespManager) {
        this.reqToRespManager = reqToRespManager;
        return this;
    }

    public RegistryBuilder cryptoManager(CryptoManager cryptoManager) {
        this.cryptoManager = cryptoManager;
        return this;
    }

    public NodeRegistry build() {
        if (StrUtil.isBlank(local.getHost())) {
            local.setHost("localhost");
        }
        if (registryConfig.getClientTimeoutSecond() < 3) {
            registryConfig.setClientTimeoutSecond(180);
        }
        registryConfig.setClientTimeoutSecond(registryConfig.getClientTimeoutSecond() / 3);
        if (registryConfig.getHeartbeatSecond() <= 0) {
            registryConfig.setHeartbeatSecond(30);
        }
        if (registryConfig.getRequestTimeoutSecond() <= 0) {
            registryConfig.setRequestTimeoutSecond(30);
        }
        // 初始化相关依赖
        if (null == contentTypeManager) {
            contentTypeManager = new BasicContentTypeManager().useDefault();
        }
        if (serializationManager == null) {
            serializationManager = new BasicSerializationManager().useDefault(contentTypeManager.getClassToKeys());
        }
        if (compressManager == null) {
            compressManager = new BasicCompressManager().useDefault();
        }
        // 初始化延迟任务管理器
        if (null == delayManager) {
            delayManager = new BasicDelayManager();
        }
        // 初始化节点管理器
        if (null == netNodeManager) {
            netNodeManager = new BasicNetNodeManager(local, delayManager);
        }
        // 初始化请求响应关联器
        if (null == reqToRespManager) {
            reqToRespManager = new BasicReqToRespManager();
        }
        if (registryConfig.getCryptoType() == CryptoType.SSL && sslContextFactory == null) {
            sslContextFactory = new BasicSSLContextFactory();
        }
        if (registryConfig.getCryptoType() == CryptoType.PASSWORD && cryptoManager == null) {
            if (StrUtil.isBlank(registryConfig.getPassword())) {
                throw new TraverConfigurationException("加密模式为密钥且加解密管理器未设置时，password不能为空");
            }
            cryptoManager = new AesGcmCryptoManager(registryConfig.getPassword(), 128);
        }
        recorders.add(new NodeHandlerRecorder(NetNode.class, new RecordNodeHandler(netNodeManager), -3));
        recorders.add(new NodeHandlerRecorder(NodeRequest.class, new RegistryRequestHandler(registryConfig.getRequestTimeoutSecond(), (RemoteExecuteManager) netNodeManager, reqToRespManager), -2));
        recorders.add(new NodeHandlerRecorder(NodeResponse.class, new RegistryResponseHandler(reqToRespManager), -1));
        CollUtil.sort(recorders, Comparator.comparingInt(NodeHandlerRecorder::getOrder));
        if (null == serializationType) {
            serializationType = DefaultSerializationType.HESSIAN;
        }
        if (messageCreator == null) {
            messageCreator = new BasicMessageCreator(registryConfig.getCryptoType() == CryptoType.PASSWORD, compress, serializationType, contentTypeManager);
        }
        NodeRegistry registry = new BasicNodeRegistry(
                local,
                cluster,
                registryConfig,
                sslContextFactory,
                serializationManager,
                contentTypeManager,
                delayManager,
                compressManager,
                recorders,
                reqToRespManager,
                cryptoManager,
                messageCreator
        );
        if (netNodeManager instanceof BasicNetNodeManager) {
            ((BasicNetNodeManager) netNodeManager).setChangeServiceListFunction(skipId -> netNodeManager.notifyClusters(skipId, registry.getClusterClients()));
        }
        return registry;
    }
}
