package top.codings.travernet.client.factory;

import cn.hutool.core.lang.UUID;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import top.codings.travernet.client.core.NodeClient;
import top.codings.travernet.common.bean.BasicInvocation;
import top.codings.travernet.common.bean.BasicInvokeResult;
import top.codings.travernet.common.bean.Invocation;
import top.codings.travernet.common.bean.InvokeResult;
import top.codings.travernet.common.error.ProviderExecuteException;
import top.codings.travernet.common.filter.TraverNetFilter;
import top.codings.travernet.common.filter.TraverNetFilterWrapper;
import top.codings.travernet.model.node.bean.NodeRequest;
import top.codings.travernet.model.node.bean.NodeType;
import top.codings.travernet.transport.utils.MessageUtil;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Slf4j
public class RemoteMethodInterceptor implements MethodInterceptor {
    private final String id;
    private final NodeClient client;
    private final TraverNetFilterWrapper filter;

    public RemoteMethodInterceptor(String id, NodeClient client) {
        this.id = id;
        this.client = client;
        TraverNetFilter traverNetFilter = client.getNodeBeanFactory().getHeadFilter();
        if (traverNetFilter instanceof TraverNetFilterWrapper) {
            filter = (TraverNetFilterWrapper) traverNetFilter;
        } else {
            filter = new TraverNetFilterWrapper(traverNetFilter);
        }
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        Invocation invocation = new BasicInvocation(id, method, method.getParameterTypes(), args, new ConcurrentHashMap<>(), NodeType.CONSUMER);
        NodeRequest request = new NodeRequest();
        request.setArgTypes(Arrays.stream(method.getParameterTypes()).map(Class::getName).toArray(String[]::new));
        request.setArgs(args);
        request.setMethod(method.getName());
        request.setServiceName(id);
        request.setSerialNo(UUID.randomUUID().toString(true));
        CompletableFuture future = new CompletableFuture();
        // 执行过滤器操作
        CompletableFuture<InvokeResult> invokeFuture = filter.invoke(() -> {
            CompletableFuture<InvokeResult> irFuture = new CompletableFuture();
            CompletableFuture<CompletableFuture<Object>> sendFuture = client.send(client.getMessageCreator().createRequest(request, invocation.getAttachments()));
            sendFuture.whenComplete((f, throwable) -> {
                if (throwable != null) {
                    log.error("发送远程执行({}#{})请求失败", id, method.getName(), throwable);
                    irFuture.complete(BasicInvokeResult.fail(throwable, null));
                    return;
                }
                if (f != null) {
                    f.whenComplete((result, err) -> {
                        Map respAtta = MessageUtil.get().getAttachments();
                        if (err != null) {
                            if ((err instanceof ProviderExecuteException) && err.getCause() != null) {
                                err = err.getCause();
                            }
                            irFuture.complete(BasicInvokeResult.fail(err, respAtta));
                            return;
                        }
                        irFuture.complete(BasicInvokeResult.success(result, respAtta));
                    });
                }
            });
            return irFuture;
        }, invocation);
        if (method.getReturnType() != CompletableFuture.class) { // 同步调用
            try {
                InvokeResult invokeResult = invokeFuture.get();
                if (invokeResult.isSuccess()) {
                    return invokeResult.returnValue();
                }
                throw invokeResult.cause();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        } else { // 异步调用
            invokeFuture.whenComplete((invokResult, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                    return;
                }
                if (invokResult.isSuccess()) {
                    future.complete(invokResult.returnValue());
                    return;
                }
                future.completeExceptionally(invokResult.cause());
            });
            return future;
        }
    }
}
