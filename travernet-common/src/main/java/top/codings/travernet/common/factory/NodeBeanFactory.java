package top.codings.travernet.common.factory;

import top.codings.travernet.common.filter.TraverNetFilter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 用于生产者管理Bean的工厂
 */
public interface NodeBeanFactory {
    Set<String> getServiceKeys();

    NodeBeanFactory registry(Object source);

    NodeBeanFactory registry(String serviceName, Object source);

    CompletableFuture<Object> execute(String className, String methodName, Class[] argTypes, Object[] args, Map<String, Object> attachments);

    NodeBeanFactory addFilter(TraverNetFilter filter);

    TraverNetFilter getHeadFilter();
}
