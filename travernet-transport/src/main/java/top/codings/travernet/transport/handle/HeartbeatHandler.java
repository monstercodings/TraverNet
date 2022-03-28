package top.codings.travernet.transport.handle;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.transport.bean.MessageCreator;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HeartbeatHandler extends IdleStateHandler {
    private final static AttributeKey<Integer> TIMEOUT_COUNT = AttributeKey.newInstance("timeoutCount");
    private final MessageCreator messageCreator;

    public HeartbeatHandler(Duration readTimeout, Duration writeTimeout) {
        this(readTimeout, writeTimeout, null);
    }

    public HeartbeatHandler(Duration readTimeout, Duration writeTimeout, MessageCreator messageCreator) {
        super(readTimeout.toMillis(), writeTimeout.toMillis(), 0, TimeUnit.MILLISECONDS);
        this.messageCreator = messageCreator;
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (evt.isFirst()) {
            ctx.channel().attr(TIMEOUT_COUNT).set(1);
        } else {
            Integer retryCount = ctx.channel().attr(TIMEOUT_COUNT).get();
            ctx.channel().attr(TIMEOUT_COUNT).set(retryCount + 1);
            // 发送心跳超过3次失败就判定为
            if (retryCount > 3) {
                if (log.isDebugEnabled()) {
                    log.debug("通道长时间未读取数据，有可能由于网络波动已断开连接 {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
                }
                ctx.close();
                return;
            }
        }
        if (messageCreator != null) {
            if (log.isTraceEnabled()) {
                log.trace("第{}次尝试发送心跳", ctx.channel().attr(TIMEOUT_COUNT).get());
            }
            // 这里使用channel进行发送的原因是ctx不会将写事件传递到链末，造成数据包编码异常
            // 而使用channel则会将写事件传递到链末，由最后的处理器发起写操作
            ctx.channel().writeAndFlush(messageCreator.createHeartbeat());
        }
    }
}
