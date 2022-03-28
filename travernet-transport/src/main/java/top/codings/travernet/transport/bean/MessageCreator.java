package top.codings.travernet.transport.bean;

import top.codings.travernet.transport.protocol.Message;

import java.util.Map;

public interface MessageCreator {

    public <T> Message<T> createRequest(T content);

    public <T> Message<T> createRequest(T content, Map attachments);

    public <T> Message<T> transferToResponse(Message message, T content);

    public Message<Void> createHeartbeat();

    public <T> Message<T> create(boolean isRequest, boolean isHeartbeat, String compressorType, String serializationType, T content, Map attachments);
}
