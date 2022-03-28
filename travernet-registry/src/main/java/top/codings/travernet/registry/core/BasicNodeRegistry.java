package top.codings.travernet.registry.core;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.NotSslRecordException;
import io.netty.handler.ssl.SslHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.client.build.NodeClientBuilder;
import top.codings.travernet.client.core.NodeClient;
import top.codings.travernet.common.bean.CryptoType;
import top.codings.travernet.common.delay.DelayManager;
import top.codings.travernet.common.error.NoConnectException;
import top.codings.travernet.common.factory.NettyEventLoopFactory;
import top.codings.travernet.common.factory.SSLContextFactory;
import top.codings.travernet.model.node.bean.CloseCommand;
import top.codings.travernet.model.node.bean.NetNode;
import top.codings.travernet.model.node.bean.NodeRequest;
import top.codings.travernet.model.node.bean.RegistryNode;
import top.codings.travernet.model.node.manage.ReqToRespManager;
import top.codings.travernet.registry.bean.RegistryConfig;
import top.codings.travernet.registry.handle.RegistryNodeHandler;
import top.codings.travernet.registry.handle.RegistryRequestHandler;
import top.codings.travernet.transport.bean.MessageCreator;
import top.codings.travernet.transport.bean.NodeHandlerRecorder;
import top.codings.travernet.transport.handle.HeartbeatHandler;
import top.codings.travernet.transport.handle.NodeHandleWrapper;
import top.codings.travernet.transport.manage.CompressManager;
import top.codings.travernet.transport.manage.ContentTypeManager;
import top.codings.travernet.transport.manage.CryptoManager;
import top.codings.travernet.transport.manage.SerializationManager;
import top.codings.travernet.transport.protocol.Message;
import top.codings.travernet.transport.protocol.MessageSpliter;
import top.codings.travernet.transport.protocol.NodeCodec;

import javax.crypto.BadPaddingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BasicNodeRegistry implements NodeRegistry {
    @Getter
    private MessageCreator messageCreator;
    private final RegistryNode local;
    private final Collection<NetNode> cluster;
    private final Collection<NodeClient> clusterClients;
    private volatile EventLoopGroup bossGroup;
    private volatile EventLoopGroup workerGroup;
    private volatile Channel serverChannel;
    private final ServerBootstrap serverBootstrap;
    @Getter
    private final SerializationManager serializationManager;
    @Getter
    private final ContentTypeManager contentTypeManager;
    @Getter
    private final DelayManager delayManager;
    @Getter
    private final CompressManager compressManager;
    @Getter
    private final ReqToRespManager reqToRespManager;
    private final Collection<NodeHandlerRecorder> recorders;
    private final RegistryConfig registryConfig;
    private final SSLContextFactory sslContextFactory;
    private final SSLContext sslContext;

    public BasicNodeRegistry(RegistryNode local,
                             Collection<NetNode> cluster,
                             RegistryConfig registryConfig,
                             SSLContextFactory sslContextFactory, SerializationManager serializationManager,
                             ContentTypeManager contentTypeManager,
                             DelayManager delayManager,
                             CompressManager compressManager,
                             Collection<NodeHandlerRecorder> recorders,
                             ReqToRespManager reqToRespManager,
                             CryptoManager cryptoManager,
                             MessageCreator messageCreator
    ) {
        this.sslContextFactory = sslContextFactory;
        this.messageCreator = messageCreator;
        this.cluster = cluster;
        this.registryConfig = registryConfig;
        clusterClients = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.serializationManager = serializationManager;
        this.contentTypeManager = contentTypeManager;
        this.delayManager = delayManager;
        this.compressManager = compressManager;
        this.reqToRespManager = reqToRespManager;
        this.recorders = recorders;
        NodeCodec codec = new NodeCodec(false, serializationManager, contentTypeManager, compressManager, cryptoManager);
        this.local = local;
        serverBootstrap = new ServerBootstrap()
                .channel(NettyEventLoopFactory.serverSocketChannelClass())
                .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
//                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) {
                        if (sslContext != null) { // 启用SSL加密方式
                            SSLEngine sslEngine = sslContext.createSSLEngine();
                            sslEngine.setUseClientMode(false);
                            sslEngine.setNeedClientAuth(registryConfig.isNeedClientAuth());
                            ch.pipeline().addLast(SslHandler.class.getName(), new SslHandler(sslEngine));
                        }
                        ch.pipeline()
                                .addLast(HeartbeatHandler.class.getSimpleName(), new HeartbeatHandler(Duration.ofSeconds(registryConfig.getClientTimeoutSecond()), Duration.ofMillis(0)))
                                .addLast(MessageSpliter.class.getSimpleName(), new MessageSpliter())
                                .addLast(NodeCodec.class.getSimpleName(), codec)
                                .addLast(NodeHandleWrapper.class.getSimpleName(), new NodeHandleWrapper(BasicNodeRegistry.this, recorders))
                                .addLast("ErrorHandler", new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        String host = ctx.channel().remoteAddress().toString();
                                        if (StrUtil.startWith(host, "/")) {
                                            host = host.substring(1);
                                        }
                                        if (ExceptionUtil.isCausedBy(cause, SSLHandshakeException.class, NotSslRecordException.class)) {
                                            log.warn("[TraverNet] SSL握手认证失败(来源 {})，请检查客户端证书配置是否正确，或排查是否有异常攻击", host);
                                        } else if (ExceptionUtil.isCausedBy(cause, BadPaddingException.class)) {
                                            log.warn("[TraverNet] 解密传输内容失败(来源 {})，请检查客户端密钥配置是否正确，或排查是否有异常攻击", host);
                                            Message<CloseCommand> message = messageCreator.createRequest(new CloseCommand("密钥不正确，请检查密钥并确保配置正确后再重试"));
                                            message.getProperty().setEncrypt(false);
                                            ctx.channel().writeAndFlush(message)
                                                    .addListener(ChannelFutureListener.CLOSE);
                                        } else {
                                            log.error("[TraverNet] 业务处理发生异常", cause);
                                        }
                                    }
                                });
                    }
                });
        CryptoType cryptoType = registryConfig.getCryptoType();
        if (cryptoType == CryptoType.SSL) {
            log.info("[TraverNet] {}服务端开启SSL认证配置", local.getNodeType().getDesc());
            if (StrUtil.isAllNotBlank(registryConfig.getServerSslCertPath(), registryConfig.getServerSslCertPassword())) {
                sslContext = sslContextFactory.createSSLContext(
                        registryConfig.getServerSslCertPath(),
                        registryConfig.getServerSslCertPassword(),
                        registryConfig.getSslProtocol()
                );
            } else {
                sslContext = sslContextFactory.createSSLContext((InputStream) null, null, registryConfig.getSslProtocol());
            }
        } else {
            sslContext = null;
            if (cryptoType == CryptoType.PASSWORD) {
                log.info("[TraverNet] {}服务端开启密钥认证配置", local.getNodeType().getDesc());
            }
        }
    }

    @Override
    public CompletableFuture<Channel> start() {
        if (bossGroup != null) {
            if (serverChannel != null) {
                return CompletableFuture.completedFuture(serverChannel);
            } else {
                return CompletableFuture.failedFuture(new NoConnectException("服务端正在启动中，请稍后"));
            }
        }
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        try {
            bossGroup = NettyEventLoopFactory.eventLoopGroup(1, "boss");
            workerGroup = NettyEventLoopFactory.eventLoopGroup(Runtime.getRuntime().availableProcessors() + 1, "worker");
            serverBootstrap.clone().group(bossGroup, workerGroup).bind(local.getHost(), local.getPort()).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (!channelFuture.isSuccess()) {
                        bossGroup.shutdownGracefully();
                        bossGroup = null;
                        workerGroup.shutdownGracefully();
                        workerGroup = null;
                        completableFuture.completeExceptionally(channelFuture.cause());
                        return;
                    }
                    serverChannel = channelFuture.channel();
                    completableFuture.complete(serverChannel);
                    connectClusters();
                }
            });
        } catch (Exception e) {
            completableFuture.completeExceptionally(e);
        }
        return completableFuture;
    }

    /**
     * 连接集群其他注册中心
     */
    private void connectClusters() {
        for (NetNode netNode : cluster) {
            NodeClient<Channel> client = NodeClientBuilder.create()
                    .local(local)
                    .target(netNode)
                    .retryIntervalMs(3000)
                    .retryMaxCount(30)
                    .heartbeatSecond(registryConfig.getHeartbeatSecond())
                    .delayManager(delayManager)
                    .serializationManager(serializationManager)
                    .contentTypeManager(contentTypeManager)
                    .compressManager(compressManager)
                    .reqToRespManager(reqToRespManager)
                    .encryptType(registryConfig.getCryptoType())
                    .password(registryConfig.getPassword())
                    .clientSslCertPath(registryConfig.getClientSslCertPath())
                    .clientSslCertPassword(registryConfig.getClientSslCertPassword())
                    .sslProtocol(registryConfig.getSslProtocol())
                    .nodeHandler(NodeRequest.class, (message, ctx, nodeContext) -> recorders.stream()
                            .filter(recorder -> recorder.getNodeHandler() instanceof RegistryRequestHandler)
                            .findFirst()
                            .ifPresent(recorder -> recorder.getNodeHandler().handle(message, ctx, nodeContext)), -100)
                    .nodeHandler(RegistryNode.class, new RegistryNodeHandler(local.getId()))
                    .build();
            log.info("[TraverNet] 开始连接注册中心集群({}:{})", netNode.getHost(), netNode.getPort());
            client.start().whenComplete((ch, t) -> {
                if (t != null) {
                    log.error("[TraverNet] 连接注册中心({}:{})失败", netNode.getHost(), netNode.getPort(), t);
                    return;
                }
                clusterClients.add(client);
            });
        }
    }

    @Override
    public CompletableFuture<Void> close() {
        if (null == serverChannel) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture completableFuture = new CompletableFuture();
        try {
            serverChannel.close();
            serverChannel = null;
            bossGroup.shutdownGracefully();
            bossGroup = null;
            workerGroup.shutdownGracefully();
            workerGroup = null;
            completableFuture.complete(null);
        } catch (Exception e) {
            completableFuture.completeExceptionally(e);
        }
        return completableFuture;
    }

    @Override
    public NetNode getNetNode() {
        return local;
    }

    @Override
    public boolean isStop() {
        return serverChannel == null || !serverChannel.isActive();
    }

    @Override
    public Collection<NodeClient> getClusterClients() {
        return clusterClients;
//        return Collections.unmodifiableCollection(clusterClients);
    }
}
