package top.codings.travernet.transport.protocol;

import cn.hutool.core.map.MapUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.model.node.bean.NodeRequest;
import top.codings.travernet.model.node.bean.NodeResponse;
import top.codings.travernet.transport.compress.Compress;
import top.codings.travernet.transport.manage.CompressManager;
import top.codings.travernet.transport.manage.ContentTypeManager;
import top.codings.travernet.transport.manage.CryptoManager;
import top.codings.travernet.transport.manage.SerializationManager;
import top.codings.travernet.transport.serialization.Serialization;
import top.codings.travernet.transport.utils.MessageUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ChannelHandler.Sharable
public class NodeCodec extends MessageToMessageCodec<ByteBuf, Message> {
    private final int magic = 0x77198206;
    public final static int LENGTH_FIELD_OFFSET = 15;
    public final static int LENGTH_FIELD_LENGTH = 4;
    private final SerializationManager serializationManager;
    private final ContentTypeManager contentTypeManager;
    private final CompressManager compressManager;
    private final CryptoManager cryptoManager;
    private final boolean parseAttchments;

    public NodeCodec(boolean parseAttchments, SerializationManager serializationManager, ContentTypeManager contentTypeManager, CompressManager compressManager, CryptoManager cryptoManager) {
        this.parseAttchments = parseAttchments;
        this.serializationManager = serializationManager;
        this.contentTypeManager = contentTypeManager;
        this.compressManager = compressManager;
        this.cryptoManager = cryptoManager;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Message message, List<Object> list) throws Exception {
        ByteBuf buffer = channelHandlerContext.alloc().buffer();
        Header header = message.getHeader();
        MessageProperty property = message.getProperty();
        // 4位魔数
        buffer.writeInt(magic);
        // 1位版本号
        buffer.writeByte(header.getVersion());
        // 1位额外信息
        buffer.writeByte(property.extraInfo());
        // 1位内容类型
        buffer.writeByte(header.getContentType());
        // 8位消息ID
        buffer.writeLong(header.getMessageId());
        if (property.isHeartbeat()) {
            buffer.writeInt(0);
            list.add(buffer);
            return;
        }
        Object contentObj = message.getContent();
        Map attachments = message.getAttachments();
        if (attachments == null) {
            attachments = new ConcurrentHashMap<>();
            message.setAttachments(attachments);
        }
        // 此处针对中转到注册中心做了特殊处理，将具体对象转移到附件中，防止没有依赖类导致反序列化时报错
        if (contentObj instanceof NodeRequest && ((NodeRequest) contentObj).getArgs() != null) {
            attachments.put("remoteArgs", ((NodeRequest) contentObj).getArgs());
            ((NodeRequest) contentObj).setArgs(null);
        } else if (contentObj instanceof NodeResponse && ((NodeResponse) contentObj).getData() != null) {
            attachments.put("responseData", ((NodeResponse) contentObj).getData());
            ((NodeResponse) contentObj).setData(null);
        }
        Serialization serialization = serializationManager.get(property.getSerializationType());
        if (null == serialization) {
            log.warn("序列化对象({})未注册", property.getSerializationType());
            ReferenceCountUtil.release(buffer);
            return;
        }
        Compress compress = compressManager.get(property.getCompressorType());
        if (null == compress) {
            log.warn("压缩对象({})未注册", property.getCompressorType());
            ReferenceCountUtil.release(buffer);
            return;
        }
        // 序列化为字节数组
        byte[] content = serialization.serialize(message.getContent());
        if (log.isTraceEnabled()) {
            log.trace("[TraverNet] 序列化后的内容大小 -> {}字节", content.length);
        }
        if (property.isEncrypt() && cryptoManager != null) {
            // 加密内容
            content = cryptoManager.encrypt(content);
        }
        // 压缩内容
        content = compress.compress(content);
        if (log.isTraceEnabled()) {
            log.trace("[TraverNet] 压缩后的内容大小 -> {}字节", content.length);
        }
        if (MapUtil.isNotEmpty(attachments)) {
            if (attachments.containsKey("rawAttachments")) {
                byte[] rawAttachments = (byte[]) attachments.remove("rawAttachments");
                buffer.writeInt(content.length + 4 + rawAttachments.length);
                buffer.writeInt(content.length);
                buffer.writeBytes(content);
                buffer.writeBytes(rawAttachments);
            } else {
                Message attachmentMsg = new Message();
                attachmentMsg.setAttachments(attachments);
                // 序列化透传内容为字节数组
                byte[] attachmentBytes = serialization.serialize(attachmentMsg);
                if (log.isTraceEnabled()) {
                    log.trace("[TraverNet] 序列化后的附件大小 ->  {}字节", attachmentBytes.length);
                }
                if (property.isEncrypt() && cryptoManager != null) {
                    // 加密透传内容
                    attachmentBytes = cryptoManager.encrypt(attachmentBytes);
                }
                // 压缩透传内容
                attachmentBytes = compress.compress(attachmentBytes);
                buffer.writeInt(content.length + 4 + attachmentBytes.length);
                buffer.writeInt(content.length);
                buffer.writeBytes(content);
                buffer.writeBytes(attachmentBytes);
            }
        } else {
            buffer.writeInt(content.length + 4);
            buffer.writeInt(content.length);
            buffer.writeBytes(content);
        }
        list.add(buffer);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        int allSize = byteBuf.readableBytes();
        if (allSize < LENGTH_FIELD_OFFSET) {
            return;
        }
        // 记录当前readIndex指针的位置，方便重置
        byteBuf.markReaderIndex();
        int i = byteBuf.readInt();
        if (i != magic) {
            // 重置readIndex指针
            byteBuf.resetReaderIndex();
            log.warn("无法识别的消息 -> {}", magic);
            return;
        }
        Message message = new Message();
        byte version = byteBuf.readByte();
        byte extraInfo = byteBuf.readByte();
        MessageProperty property = MessageProperty.parse(extraInfo);
        if (property.isHeartbeat()) {
            // 心跳消息可以直接丢弃
            byteBuf.resetReaderIndex();
            if (log.isTraceEnabled()) {
                log.trace("接收到客户端心跳信息");
            }
            return;
        }
        byte contentType = byteBuf.readByte();
        long messageId = byteBuf.readLong();
        Header header = new Header();
        header.setMessageId(messageId);
        header.setVersion(version);
        header.setContentType(contentType);

        int messageAndAttachmentSize = byteBuf.readInt();
        Object content;
        int messageSize = byteBuf.readInt();
        header.setSize(messageSize);
        Serialization serialization = serializationManager.get(property.getSerializationType());
        if (null == serialization) {
            byteBuf.resetReaderIndex();
            log.warn("序列化对象({})未注册", property.getSerializationType());
            return;
        }
        Class contentClass = contentTypeManager.get(contentType);
        if (contentClass == null) {
            byteBuf.resetReaderIndex();
            log.warn("内容对象({})未注册", contentType);
            return;
        }
        Compress compress = compressManager.get(property.getCompressorType());
        if (null == compress) {
            byteBuf.resetReaderIndex();
            log.warn("压缩对象({})未注册", property.getCompressorType());
            return;
        }
        byte[] data = new byte[messageSize];
        byteBuf.readBytes(data);
        // 解压缩数据
        data = compress.uncompress(data);
        if (property.isEncrypt() && null != cryptoManager) {
            // 解密数据
            data = cryptoManager.decrypt(data);
        }
        // 反序列化对象内容
        content = serialization.deSerialize(data, contentClass);
        if (messageAndAttachmentSize - messageSize > 4) {
            // 说明带有透传数据
            data = new byte[messageAndAttachmentSize - 4 - messageSize];
            byteBuf.readBytes(data);
            if (parseAttchments) {
                data = compress.uncompress(data);
                if (property.isEncrypt() && null != cryptoManager) {
                    // 解密数据
                    data = cryptoManager.decrypt(data);
                }
                // 反序列化对象内容
                Map attachments = serialization.deSerialize(data, Message.class).getAttachments();
                message.setAttachments(attachments);
                if (content instanceof NodeRequest) {
                    Object remoteArgs = attachments.remove("remoteArgs");
                    if (remoteArgs instanceof Collection) {
                        ((NodeRequest) content).setArgs(((Collection) remoteArgs).toArray());
                    } else {
                        ((NodeRequest) content).setArgs((Object[]) remoteArgs);
                    }
                } else if (content instanceof NodeResponse) {
                    ((NodeResponse) content).setData(attachments.remove("responseData"));
                }
            } else {
                Map attachments = new ConcurrentHashMap();
                attachments.put("rawAttachments", data);
                message.setAttachments(attachments);
            }
        } else {
            message.setAttachments(new ConcurrentHashMap<>());
        }
        message.setHeader(header);
        message.setProperty(property);
        message.setContent(content);
        MessageUtil.set(message);
        list.add(content);
    }
}
