package top.codings.travernet.common.filter.support;

import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.common.bean.BasicInvokeResult;
import top.codings.travernet.common.bean.Invocation;
import top.codings.travernet.common.bean.InvokeResult;
import top.codings.travernet.common.bean.Invoker;
import top.codings.travernet.common.error.MockServiceException;
import top.codings.travernet.common.error.RpcException;
import top.codings.travernet.common.filter.TraverNetOrderFilter;
import top.codings.travernet.model.node.bean.NodeType;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 因网络原因远端调用失败时模拟服务响应的过滤器
 */
@Slf4j
public class MockServiceFilter implements TraverNetOrderFilter {
    private final Map<Class, Object> mockMap = new ConcurrentHashMap<>();

    public MockServiceFilter registryMockService(Object mock) {
        Class<?>[] interfaces = mock.getClass().getInterfaces();
        for (Class<?> anInterface : interfaces) {
            mockMap.put(anInterface, mock);
        }
        return this;
    }

    @Override
    public CompletableFuture<InvokeResult> invoke(Invoker invoker, Invocation invocation) {
        if (invocation.getNodeType() != NodeType.CONSUMER) {
            return invoker.invoke();
        }
        return invoker.invoke().thenApply(invokeResult -> {
            Map<String, Object> attachments = invokeResult.getAttachments();
            if (invokeResult.isSuccess()) {
                return invokeResult;
            } else if (invokeResult.cause() instanceof RpcException) {
                Method method = invocation.getMethod();
                Class<?> declaringClass = method.getDeclaringClass();
                Object mock = mockMap.get(declaringClass);
                if (null == mock) {
                    return invokeResult;
                }
                if (log.isDebugEnabled()) {
                    log.debug("[TraverNet] RPC远程执行失败,使用Mock对象返回预置数据");
                }
                try {
                    Class<?> returnType = method.getReturnType();
                    Object invokeReturn = method.invoke(mock, invocation.getArgs());
                    if (returnType == CompletableFuture.class) {
                        Object o = ((CompletableFuture) invokeReturn).get();
                        return BasicInvokeResult.success(o, attachments);
                    } else {
                        return BasicInvokeResult.success(invokeReturn, attachments);
                    }
                } catch (Exception e) {
                    return BasicInvokeResult.fail(new MockServiceException(e), invokeResult.getAttachments());
                }
            } else {
                return invokeResult;
            }
        });
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }
}
