package top.codings.travernet.transport.protocol;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Header {
    private short magic; // 魔数
    private byte version; // 版本号
    private Long messageId; // 消息ID
    private Integer size; // 消息体长度
    private byte contentType; // 内容类型
}
