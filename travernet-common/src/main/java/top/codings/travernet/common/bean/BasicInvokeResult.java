package top.codings.travernet.common.bean;

import lombok.Getter;

import java.util.Map;

public class BasicInvokeResult implements InvokeResult {
    @Getter
    private boolean success;
    private Object value;
    private Throwable throwable;
    private Map<String, Object> attachments;

    private BasicInvokeResult() {

    }

    public static InvokeResult success(Object value, Map<String, Object> attachments) {
        BasicInvokeResult result = new BasicInvokeResult();
        result.success = true;
        result.value = value;
        result.attachments = attachments;
        return result;
    }

    public static InvokeResult fail(Throwable throwable, Map<String, Object> attachments) {
        BasicInvokeResult result = new BasicInvokeResult();
        result.success = false;
        result.throwable = throwable;
        result.attachments = attachments;
        return result;
    }

    @Override
    public Object returnValue() {
        return value;
    }

    @Override
    public Throwable cause() {
        return throwable;
    }

    @Override
    public Map<String, Object> getAttachments() {
        return attachments;
    }
}
