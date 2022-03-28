package top.codings.travernet.client.build;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import top.codings.travernet.client.bean.ClientConfig;
import top.codings.travernet.client.core.BasicNodeClient;
import top.codings.travernet.client.core.NodeClient;
import top.codings.travernet.client.handle.ClientResponseHandler;
import top.codings.travernet.client.handle.CloseCommandHandler;
import top.codings.travernet.client.handle.ProviderRequestHandler;
import top.codings.travernet.common.bean.CryptoType;
import top.codings.travernet.common.delay.BasicDelayManager;
import top.codings.travernet.common.delay.DelayManager;
import top.codings.travernet.common.error.TraverConfigurationException;
import top.codings.travernet.common.factory.BasicNodeBeanFactory;
import top.codings.travernet.common.factory.BasicSSLContextFactory;
import top.codings.travernet.common.factory.NodeBeanFactory;
import top.codings.travernet.common.factory.SSLContextFactory;
import top.codings.travernet.model.node.bean.CloseCommand;
import top.codings.travernet.model.node.bean.NetNode;
import top.codings.travernet.model.node.bean.NodeRequest;
import top.codings.travernet.model.node.bean.NodeResponse;
import top.codings.travernet.model.node.manage.BasicReqToRespManager;
import top.codings.travernet.model.node.manage.ReqToRespManager;
import top.codings.travernet.transport.bean.*;
import top.codings.travernet.transport.handle.NodeHandler;
import top.codings.travernet.transport.manage.*;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NodeClientBuilder {
    private NetNode local;
    private NetNode target;
    private DelayManager delayManager;
    private SerializationManager serializationManager;
    private ContentTypeManager contentTypeManager;
    private CompressManager compressManager;
    private NodeBeanFactory nodeBeanFactory;
    private ReqToRespManager reqToRespManager;
    private CryptoManager cryptoManager;
    private SSLContextFactory sslContextFactory;
    private MessageCreator messageCreator;
    private boolean compress = true;
    private SerializationType serializationType;
    private final List<NodeHandlerRecorder> recorders = new CopyOnWriteArrayList<>();
    private final ClientConfig clientConfig = new ClientConfig();

    private NodeClientBuilder() {

    }

    public final static NodeClientBuilder create() {
        NodeClientBuilder builder = new NodeClientBuilder();
        return builder;
    }

    public NodeClientBuilder local(NetNode local) {
        this.local = local;
        return this;
    }

    public NodeClientBuilder target(NetNode target) {
        this.target = target;
        return this;
    }

    public NodeClientBuilder retryIntervalMs(long retryIntervalMs) {
        clientConfig.setRetryIntervalMs(retryIntervalMs);
        return this;
    }

    public NodeClientBuilder retryMaxCount(int retryMaxCount) {
        clientConfig.setRetryMaxCount(retryMaxCount);
        return this;
    }

    public NodeClientBuilder encryptType(CryptoType cryptoType) {
        clientConfig.setCryptoType(cryptoType);
        return this;
    }

    public NodeClientBuilder clientSslCertPath(String clientSslCertPath) {
        clientConfig.setClientSslCertPath(clientSslCertPath);
        return this;
    }

    public NodeClientBuilder clientSslCertPassword(String clientSslCertPassword) {
        clientConfig.setClientSslCertPassword(clientSslCertPassword);
        return this;
    }

    public NodeClientBuilder sslProtocol(String sslProtocol) {
        clientConfig.setSslProtocol(sslProtocol);
        return this;
    }

    public NodeClientBuilder password(String password) {
        clientConfig.setPassword(password);
        return this;
    }

    public NodeClientBuilder delayManager(DelayManager delayManager) {
        this.delayManager = delayManager;
        return this;
    }

    public NodeClientBuilder serializationManager(SerializationManager serializationManager) {
        this.serializationManager = serializationManager;
        return this;
    }

    public NodeClientBuilder contentTypeManager(ContentTypeManager contentTypeManager) {
        this.contentTypeManager = contentTypeManager;
        return this;
    }

    public NodeClientBuilder compressManager(CompressManager compressManager) {
        this.compressManager = compressManager;
        return this;
    }

    public NodeClientBuilder nodeBeanFactory(NodeBeanFactory nodeBeanFactory) {
        this.nodeBeanFactory = nodeBeanFactory;
        return this;
    }

    public NodeClientBuilder reqToRespManager(ReqToRespManager reqToRespManager) {
        this.reqToRespManager = reqToRespManager;
        return this;
    }

    public NodeClientBuilder cryptoManager(CryptoManager cryptoManager) {
        this.cryptoManager = cryptoManager;
        return this;
    }

    public NodeClientBuilder sslContextFactory(SSLContextFactory sslContextFactory) {
        this.sslContextFactory = sslContextFactory;
        return this;
    }

    public NodeClientBuilder messageCreator(MessageCreator messageCreator) {
        this.messageCreator = messageCreator;
        return this;
    }

    public NodeClientBuilder compress(boolean compress) {
        this.compress = compress;
        return this;
    }

    public NodeClientBuilder defaultSerializationType(SerializationType serializationType) {
        this.serializationType = serializationType;
        return this;
    }

    public NodeClientBuilder heartbeatSecond(int heartbeatSecond) {
        clientConfig.setHeartbeatSecond(heartbeatSecond);
        return this;
    }

    public NodeClientBuilder requestTimeoutSecond(int requestTimeoutSecond) {
        clientConfig.setRequestTimeoutSecond(requestTimeoutSecond);
        return this;
    }

    public <T> NodeClientBuilder nodeHandler(Class<T> aClass, NodeHandler<T> nodeHandler) {
        return nodeHandler(aClass, nodeHandler, Integer.MAX_VALUE);
    }

    public <T> NodeClientBuilder nodeHandler(Class<T> aClass, NodeHandler<T> nodeHandler, int order) {
        recorders.add(new NodeHandlerRecorder(aClass, nodeHandler, order));
        return this;
    }

    public NodeClient build() {
        if (null == delayManager) {
            delayManager = new BasicDelayManager();
        }
        if (null == contentTypeManager) {
            contentTypeManager = new BasicContentTypeManager().useDefault();
        }
        if (serializationManager == null) {
            serializationManager = new BasicSerializationManager().useDefault(contentTypeManager.getClassToKeys());
        }
        if (compressManager == null) {
            compressManager = new BasicCompressManager().useDefault();
        }
        if (reqToRespManager == null) {
            reqToRespManager = new BasicReqToRespManager();
        }
        if (nodeBeanFactory == null) {
            nodeBeanFactory = new BasicNodeBeanFactory();
        }
        if (clientConfig.getRetryIntervalMs() < 0) {
            clientConfig.setRetryIntervalMs(3000);
        }
        if (clientConfig.getRetryMaxCount() < 0) {
            clientConfig.setRetryMaxCount(0);
        }
        if (clientConfig.getHeartbeatSecond() <= 0) {
            clientConfig.setHeartbeatSecond(30);
        }
        if (clientConfig.getRequestTimeoutSecond() <= 0) {
            clientConfig.setRequestTimeoutSecond(60);
        }
        if (StrUtil.isBlank(clientConfig.getSslProtocol())) {
            clientConfig.setSslProtocol("TLSv1.3");
        }
        recorders.add(new NodeHandlerRecorder(CloseCommand.class, new CloseCommandHandler(), -3));
        recorders.add(new NodeHandlerRecorder(NodeRequest.class, new ProviderRequestHandler(nodeBeanFactory), -2));
        recorders.add(new NodeHandlerRecorder(NodeResponse.class, new ClientResponseHandler(reqToRespManager), -1));
        CollUtil.sort(recorders, Comparator.comparingInt(NodeHandlerRecorder::getOrder));
        if (clientConfig.getCryptoType() == CryptoType.SSL && sslContextFactory == null) {
            sslContextFactory = new BasicSSLContextFactory();
        }
        if (clientConfig.getCryptoType() == CryptoType.PASSWORD && cryptoManager == null) {
            if (StrUtil.isBlank(clientConfig.getPassword())) {
                throw new TraverConfigurationException("加密模式为密钥且加解密管理器未设置时，password不能为空");
            }
            cryptoManager = new AesGcmCryptoManager(clientConfig.getPassword(), 128);
        }
        if (null == serializationType) {
            serializationType = DefaultSerializationType.HESSIAN;
        }
        if (null == messageCreator) {
            messageCreator = new BasicMessageCreator(clientConfig.getCryptoType() == CryptoType.PASSWORD, compress, serializationType, contentTypeManager);
        }
        NodeClient client = new BasicNodeClient(
                local,
                target,
                clientConfig,
                sslContextFactory,
                delayManager,
                serializationManager,
                contentTypeManager,
                compressManager,
                recorders,
                nodeBeanFactory,
                reqToRespManager,
                cryptoManager,
                messageCreator
        );
        return client;
    }
}
