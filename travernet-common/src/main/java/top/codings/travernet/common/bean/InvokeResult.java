package top.codings.travernet.common.bean;

import java.util.Map;

public interface InvokeResult {
    Object returnValue();

    boolean isSuccess();

    Throwable cause();

    Map<String, Object> getAttachments();
}
