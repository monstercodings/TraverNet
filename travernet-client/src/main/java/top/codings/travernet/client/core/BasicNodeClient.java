package top.codings.travernet.client.core;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.client.bean.ClientConfig;
import top.codings.travernet.client.handle.CloseCommandHandler;
import top.codings.travernet.common.bean.CryptoType;
import top.codings.travernet.common.bean.DelayFuture;
import top.codings.travernet.common.delay.DelayManager;
import top.codings.travernet.common.error.NoConnectException;
import top.codings.travernet.common.factory.NettyEventLoopFactory;
import top.codings.travernet.common.factory.NodeBeanFactory;
import top.codings.travernet.common.factory.SSLContextFactory;
import top.codings.travernet.model.node.bean.NetNode;
import top.codings.travernet.model.node.bean.NodeSerial;
import top.codings.travernet.model.node.bean.ProviderNode;
import top.codings.travernet.model.node.manage.ReqToRespManager;
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class BasicNodeClient extends RetryNodeClient<CompletableFuture<Object>> {
    @Getter
    private final MessageCreator messageCreator;
    private final NetNode local;
    @Getter
    private final NetNode target;
    private final Bootstrap clientBootstrap;
    private volatile EventLoopGroup worker;
    private volatile Channel channel;
    private volatile boolean manualClose;
    @Getter
    private final SerializationManager serializationManager;
    @Getter
    private final ContentTypeManager contentTypeManager;
    @Getter
    private final CompressManager compressManager;
    @Getter
    private final NodeBeanFactory nodeBeanFactory;
    @Getter
    private final ReqToRespManager reqToRespManager;
    private final ClientConfig clientConfig;
    private final SSLContextFactory sslContextFactory;
    private final SSLContext sslContext;
    private final CloseCommandHandler closeCommandHandler;

    public BasicNodeClient(NetNode local,
                           NetNode target,
                           ClientConfig clientConfig,
                           SSLContextFactory sslContextFactory,
                           DelayManager delayManager,
                           SerializationManager serializationManager,
                           ContentTypeManager contentTypeManager,
                           CompressManager compressManager,
                           Collection<NodeHandlerRecorder> recorders,
                           NodeBeanFactory nodeBeanFactory,
                           ReqToRespManager reqToRespManager,
                           CryptoManager cryptoManager,
                           MessageCreator messageCreator
    ) {
        super(clientConfig.getRetryIntervalMs(), clientConfig.getRetryMaxCount(), delayManager);
        this.sslContextFactory = sslContextFactory;
        this.clientConfig = clientConfig;
        this.target = target;
        this.local = local;
        this.serializationManager = serializationManager;
        this.contentTypeManager = contentTypeManager;
        this.compressManager = compressManager;
        this.nodeBeanFactory = nodeBeanFactory;
        this.messageCreator = messageCreator;
        {
            Optional<NodeHandlerRecorder> optional = recorders.stream().filter(recorder -> {
                if (recorder.getNodeHandler() instanceof CloseCommandHandler) {
                    return true;
                }
                return false;
            }).findFirst();
            if (optional.isPresent()) {
                closeCommandHandler = (CloseCommandHandler) optional.get().getNodeHandler();
            } else {
                closeCommandHandler = null;
            }
        }
        clientBootstrap = new Bootstrap()
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .channel(NettyEventLoopFactory.socketChannelClass())
                .handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) {
                        if (sslContext != null) {
                            SSLEngine sslEngine = sslContext.createSSLEngine();
                            sslEngine.setUseClientMode(true);
                            ch.pipeline().addLast(new SslHandler(sslEngine));
                        }
                        ch.pipeline()
                                .addLast(HeartbeatHandler.class.getSimpleName(), new HeartbeatHandler(Duration.ofMillis(0), Duration.ofSeconds(clientConfig.getHeartbeatSecond()), messageCreator))
                                .addLast(MessageSpliter.class.getSimpleName(), new MessageSpliter())
                                .addLast(NodeCodec.class.getSimpleName(), new NodeCodec(true, BasicNodeClient.this.serializationManager, BasicNodeClient.this.contentTypeManager, BasicNodeClient.this.compressManager, cryptoManager))
                                .addLast(NodeHandleWrapper.class.getSimpleName(), new NodeHandleWrapper(BasicNodeClient.this, recorders))
                                .addLast("ErrorHandler", new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        if (ExceptionUtil.isCausedBy(cause, SSLHandshakeException.class)) {
                                            close();
                                            log.error("[TraverNet] SSL握手失败", cause);
                                        } else {
                                            log.error("[TraverNet] 业务处理发生异常", cause);
                                        }
                                    }
                                })
                        ;
                    }
                });
        this.reqToRespManager = reqToRespManager;
        CryptoType cryptoType = clientConfig.getCryptoType();
        if (cryptoType == CryptoType.SSL) {
            log.info("[TraverNet] {}客户端开启SSL认证配置", local.getNodeType().getDesc());
            if (StrUtil.isAllNotBlank(clientConfig.getClientSslCertPath(), clientConfig.getClientSslCertPassword())) {
                sslContext = sslContextFactory.createSSLContext(
                        clientConfig.getClientSslCertPath(),
                        clientConfig.getClientSslCertPassword(),
                        clientConfig.getSslProtocol()
                );
            } else {
                sslContext = sslContextFactory.createSSLContext((InputStream) null, null, clientConfig.getSslProtocol());
            }
        } else {
            sslContext = null;
            if (cryptoType == CryptoType.PASSWORD) {
                if (cryptoType == CryptoType.PASSWORD) {
                    log.info("[TraverNet] {}客户端开启密钥认证配置", local.getNodeType().getDesc());
                }
            }
        }
    }

    @Override
    public CompletableFuture<Channel> doStart() {
        if (worker != null) {
            if (channel != null) {
                return CompletableFuture.completedFuture(channel);
            } else {
                return CompletableFuture.failedFuture(new NoConnectException("客户端正在连接中，请稍后"));
            }
        }
        manualClose = false;
        CompletableFuture completableFuture = new CompletableFuture();
        worker = NettyEventLoopFactory.eventLoopGroup(Runtime.getRuntime().availableProcessors() + 1, "client");
        clientBootstrap.clone().group(worker).connect(target.getHost(), target.getPort())
                .addListener((ChannelFutureListener) channelFuture -> {
                    if (!channelFuture.isSuccess()) {
                        worker.shutdownGracefully();
                        worker = null;
                        completableFuture.completeExceptionally(channelFuture.cause());
                        return;
                    }
                    channel = channelFuture.channel();
                    channel.closeFuture().addListener(future ->
                            getDelayManager().delay(new DelayFuture<>(500))
                                    .whenComplete((o, t) -> {
                                        if (t != null || manualClose) {
                                            log.warn("[TraverNet] 与服务端的连接已断开，如有需要请手动重新连接");
                                            return;
                                        }
                                        if (null != closeCommandHandler && closeCommandHandler.isForcedClose()) {
                                            closeCommandHandler.reset();
                                            close();
                                            return;
                                        }
                                        // 断线重连
                                        close().whenComplete((a, throwable) -> start().whenComplete((b, connectError) -> {
                                            if (connectError != null) {
                                                log.error("[TraverNet] 重连注册中心({}:{})失败", target.getHost(), target.getPort(), connectError);
                                                return;
                                            }
                                            log.info("[TraverNet] 重连注册中心({}:{})成功", target.getHost(), target.getPort());
                                        }));
                                    }));
                    refresh();
                    completableFuture.complete(channel);
                });
        return completableFuture;
    }

    @Override
    protected CompletableFuture<CompletableFuture<Object>> doSend(Message message) {
        if (null == channel) {
            return CompletableFuture.failedFuture(new NoConnectException("客户端尚未连接到服务端"));
        }
        CompletableFuture<CompletableFuture<Object>> completableFuture = new CompletableFuture();
        channel.writeAndFlush(message).addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                Object content = message.getContent();
                if (content instanceof NodeSerial) {
                    CompletableFuture<Object> cache = reqToRespManager.cache((NodeSerial) content, Duration.ofSeconds(clientConfig.getRequestTimeoutSecond()));
                    completableFuture.complete(cache);
                } else {
                    completableFuture.complete(null);
                }
            } else {
                completableFuture.completeExceptionally(channelFuture.cause());
            }
        });
        return completableFuture;
    }

    @Override
    public CompletableFuture<Void> close() {
        if (channel == null) {
            return CompletableFuture.failedFuture(new NoConnectException("尚未连接服务端"));
        }
        CompletableFuture completableFuture = new CompletableFuture();
        try {
            manualClose = true;
            channel.close();
            channel = null;
            worker.shutdownGracefully();
            worker = null;
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
        return channel == null || !channel.isActive();
    }

    @Override
    public void refresh() {
        if (local instanceof ProviderNode) {
            nodeBeanFactory.getServiceKeys().forEach(((ProviderNode) local)::registry);
        }
        Message<NetNode> request = messageCreator.createRequest(local);
        send(request).whenComplete((channel, throwable) -> {
            if (throwable != null) {
                log.error("发送本地节点信息失败", throwable);
                return;
            }
        });
    }
}