package top.codings.travernet.transport.protocol;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageProperty {
    private boolean request;
    private String serializationType;
    private String compressorType;
    private boolean heartbeat;
    private boolean encrypt;

    public final static MessageProperty parse(byte extraInfo) {
        String hex = Integer.toBinaryString(extraInfo);
        // 0-01-01-01 请求 01类型序列化方式 01类型压缩方式 01心跳
        hex = String.format("%07d", Integer.valueOf(hex));
        String reqType = hex.substring(0, 1);
        String serializationType = hex.substring(1, 3);
        String compressorType = hex.substring(3, 5);
        String messageHeartbeatType = hex.substring(5, 6);
        String encryptEnabled = hex.substring(6, 7);
        return MessageProperty.builder()
                .request("0".equals(reqType))
                .serializationType(serializationType)
                .compressorType(compressorType)
                .heartbeat("1".equals(messageHeartbeatType))
                .encrypt("1".equals(encryptEnabled))
                .build();
    }

    public byte extraInfo() {
        return Integer.valueOf((request ? "0" : "1") + serializationType + compressorType + (heartbeat ? "1" : "0") + (encrypt ? "1" : "0"), 2).byteValue();
    }
}
