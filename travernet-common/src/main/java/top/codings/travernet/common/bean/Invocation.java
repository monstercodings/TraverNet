package top.codings.travernet.common.bean;

import top.codings.travernet.model.node.bean.NodeType;

import java.lang.reflect.Method;
import java.util.Map;

public interface Invocation {
    String getServiceName();

    Method getMethod();

    Class<?>[] getParameterTypes();

    Object[] getArgs();

    Map<String, Object> getAttachments();

    NodeType getNodeType();
}
