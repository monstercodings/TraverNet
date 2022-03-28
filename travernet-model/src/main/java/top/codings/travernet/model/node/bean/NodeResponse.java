package top.codings.travernet.model.node.bean;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

public class NodeResponse implements NodeSerial, Serializable {
    @Getter
    private final String serialNo;
    @Getter
    private boolean success;
    @Getter
    @Setter
    private String message;
    @Getter
    @Setter
    private Throwable throwable;
    @Getter
    @Setter
    private Object data;

    public NodeResponse(String serialNo, Object data) {
        this.serialNo = serialNo;
        success = true;
        message = "请求成功";
        this.data = data;
    }

    public NodeResponse(String serialNo, Throwable throwable) {
        this.serialNo = serialNo;
        success = false;
        message = "请求失败";
        this.throwable = throwable;
    }

    public Throwable cause() {
        return throwable;
    }
}
