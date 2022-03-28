package top.codings.travernet.model.node.bean;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class NodeRequest implements NodeSerial, Serializable {
    private String serialNo;
    private String serviceName;
    private String method;
    private String[] argTypes;
    private Object[] args;
}
