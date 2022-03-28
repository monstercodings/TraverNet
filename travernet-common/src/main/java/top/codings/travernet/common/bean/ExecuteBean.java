package top.codings.travernet.common.bean;

import lombok.Builder;
import lombok.Getter;

import java.lang.reflect.Method;

@Getter
@Builder
public class ExecuteBean {
    private Object source;
    private Method method;
    private boolean allow;
}
