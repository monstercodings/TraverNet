package top.codings.travernet.common.bean;

import lombok.Getter;
import top.codings.travernet.model.node.bean.NodeType;

import java.lang.reflect.Method;
import java.util.Map;

@Getter
public class BasicInvocation implements Invocation {
    private final String serviceName;
    private final Method method;
    private final Class<?>[] parameterTypes;
    private final Object[] args;
    private final Map<String, Object> attachments;
    private final NodeType nodeType;

    public BasicInvocation(String serviceName, Method method, Class<?>[] parameterTypes, Object[] args, Map<String, Object> attachments, NodeType nodeType) {
        this.serviceName = serviceName;
        this.method = method;
        this.parameterTypes = parameterTypes;
        this.args = args;
        this.attachments = attachments;
        this.nodeType = nodeType;
    }
}
