package top.codings.travernet.transport.bean;

import cn.hutool.core.map.MapUtil;
import top.codings.travernet.common.error.ContentTypeException;
import top.codings.travernet.transport.manage.ContentTypeManager;
import top.codings.travernet.transport.protocol.Header;
import top.codings.travernet.transport.protocol.Message;
import top.codings.travernet.transport.protocol.MessageProperty;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class BasicMessageCreator implements MessageCreator {
    private final AtomicLong index = new AtomicLong(0);
    private final boolean encrypt;
    private final String compressType;
    private final SerializationType defaultSerializationType;
    private final ContentTypeManager contentTypeManager;

    public BasicMessageCreator(boolean encrypt, boolean compress, SerializationType defaultSerializationType, ContentTypeManager contentTypeManager) {
        this.encrypt = encrypt;
        this.compressType = compress ? "01" : "00";
        this.defaultSerializationType = defaultSerializationType;
        this.contentTypeManager = contentTypeManager;
    }

    @Override
    public <T> Message<T> createRequest(T content) {
        return createRequest(content, null);
    }

    @Override
    public <T> Message<T> createRequest(T content, Map attachments) {
        return create(true, false, compressType, defaultSerializationType.getValue(), content, attachments);
    }

    @Override
    public <T> Message<T> transferToResponse(Message message, T content) {
        message.getProperty().setRequest(false);
        message.getHeader().setSize(0);
        message.getHeader().setContentType(contentTypeManager.get(content.getClass()));
        message.setContent(content);
        return message;
    }

    @Override
    public Message<Void> createHeartbeat() {
        return create(true, true, compressType, defaultSerializationType.getValue(), null, null);
    }

    @Override
    public <T> Message<T> create(boolean isRequest, boolean isHeartbeat, String compressorType, String serializationType, T content, Map attachments) {
        Message message = new Message();
        if (MapUtil.isNotEmpty(attachments)) {
            message.setAttachments(attachments);
        }
        Header header = new Header();
        header.setVersion((byte) 1);
        if (isHeartbeat) {
            header.setContentType((byte) 0);
        } else {
            if (null == content) {
                throw new ContentTypeException("非心跳消息必须携带有效数据载体(即content不能为空)");
            }
            header.setContentType(contentTypeManager.get(content.getClass()));
        }
        message.setHeader(header);
        MessageProperty property = MessageProperty.builder()
                .request(isRequest)
                .heartbeat(isHeartbeat)
                .compressorType(compressorType)
                .serializationType(serializationType)
                .encrypt(encrypt)
                .build();
        message.setProperty(property);
        message.setContent(content);
        header.setMessageId(index.getAndIncrement());
        return message;
    }
}
