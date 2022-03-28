package top.codings.travernet.transport.bean;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DefaultSerializationType implements SerializationType {
    PROTOSTUFF("00"),
    HESSIAN("01"),
    KRYO("10"),
    FASTJSON("11"),
    ;
    private String value;
}
