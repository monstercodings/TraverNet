package top.codings.travernet.transport.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageSpliter extends LengthFieldBasedFrameDecoder {
    public MessageSpliter() {
        super(Integer.MAX_VALUE, NodeCodec.LENGTH_FIELD_OFFSET, NodeCodec.LENGTH_FIELD_LENGTH, 0, 0);
    }
}
