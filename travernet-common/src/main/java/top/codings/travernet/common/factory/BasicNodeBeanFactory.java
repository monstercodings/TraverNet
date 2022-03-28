package top.codings.travernet.common.factory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.common.annotation.NotAllow;
import top.codings.travernet.common.annotation.TraverService;
import top.codings.travernet.common.bean.*;
import top.codings.travernet.common.error.NoProviderException;
import top.codings.travernet.common.error.ProviderExecuteException;
import top.codings.travernet.common.error.ServiceNameExsitException;
import top.codings.travernet.common.filter.TraverNetFilter;
import top.codings.travernet.common.filter.TraverNetFilterWrapper;
import top.codings.travernet.common.filter.support.NopFilter;
import top.codings.travernet.model.node.bean.NodeType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class BasicNodeBeanFactory implements NodeBeanFactory {
    /**
     * 别名关联执行对象
     */
    private final Map<String, Collection<ExecuteBean>> idToExecuteBeanMap = new ConcurrentHashMap<>();
    private final TraverNetFilterWrapper wrapper = new TraverNetFilterWrapper(new NopFilter());

    private final ExecutorService executorService = ExecutorBuilder.create()
            .setCorePoolSize(Runtime.getRuntime().availableProcessors() + 1)
            .setMaxPoolSize(Integer.MAX_VALUE)
            .setThreadFactory(ThreadUtil.newNamedThreadFactory("RPC任务池-", true))
            .setKeepAliveTime(1, TimeUnit.MINUTES)
            .setWorkQueue(new LinkedBlockingQueue<>(Integer.MAX_VALUE))
            .build();

    @Override
    public Set<String> getServiceKeys() {
        return Collections.unmodifiableSet(idToExecuteBeanMap.keySet());
    }

    @Override
    public BasicNodeBeanFactory registry(Object source) {
        String serviceName;
        Class<?> sourceClass = source.getClass();
        if (sourceClass.isAnnotationPresent(TraverService.class)) {
            serviceName = sourceClass.getAnnotation(TraverService.class).value();
            if (StrUtil.isBlank(serviceName)) {
                serviceName = sourceClass.getName();
            }
        } else {
            serviceName = sourceClass.getName();
        }
        return registry(serviceName, source);
    }

    @Override
    public BasicNodeBeanFactory registry(String serviceName, Object source) {
        Class<?> sourceClass = source.getClass();
        Arrays.stream(sourceClass.getMethods())
                .filter(method -> method.getDeclaringClass() != Object.class)
                .forEach(method -> registry(serviceName, source, method));
        return this;
    }

    private BasicNodeBeanFactory registry(String serviceName, Object source, Method method) {
        boolean allow = true;
        if (method.isAnnotationPresent(NotAllow.class)) {
            allow = false;
        }
        Class<?> declaringClass = method.getDeclaringClass();
        if (!declaringClass.isAssignableFrom(source.getClass())) {
            throw new IllegalArgumentException(String.format("方法(%s)不属于类(%s)", method.getName(), source.getClass().getName()));
        }
        Collection<ExecuteBean> collection = idToExecuteBeanMap.computeIfAbsent(serviceName, s -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        if (CollUtil.isNotEmpty(collection) && collection.stream().anyMatch(b -> b.getSource() != source)) {
            throw new ServiceNameExsitException(String.format("服务名(%s)已存在,请保证服务名唯一", serviceName));
        }
        collection.add(ExecuteBean.builder()
                .source(source)
                .method(method)
                .allow(allow)
                .build());
        return this;
    }

    @Override
    public CompletableFuture<Object> execute(String serviceName, String methodName, Class[] argTypes, Object[] args, Map<String, Object> attachments) {
        CompletableFuture future = new CompletableFuture();
        try {
            Collection<ExecuteBean> executeBeans = idToExecuteBeanMap.get(serviceName);
            if (!CollUtil.isEmpty(executeBeans)) {
                for (ExecuteBean executeBean : executeBeans) {
                    if (verifySameMethod(methodName, argTypes, executeBean.getMethod())) {
                        if (!executeBean.isAllow()) {
                            future.completeExceptionally(new ProviderExecuteException(String.format("服务(%s)的方法(%s)不允许对外暴露", serviceName, methodName)));
                        } else {
                            Invocation invocation = new BasicInvocation(serviceName, executeBean.getMethod(), argTypes, args, attachments, NodeType.PROVIDER);
                            submitExecuteTask(future, executeBean, invocation);
                        }
                        return future;
                    }
                }
            }
            future.completeExceptionally(new NoProviderException(String.format("服务(%s)的方法(%s)尚未注册", serviceName, methodName)));
            return future;
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public synchronized NodeBeanFactory addFilter(TraverNetFilter filter) {
        TraverNetFilterWrapper last;
        if (filter instanceof TraverNetFilterWrapper) {
            last = (TraverNetFilterWrapper) filter;
        } else {
            last = new TraverNetFilterWrapper(filter);
        }
        TraverNetFilterWrapper now = wrapper;
        while (true) {
            TraverNetFilterWrapper next = now.getNext();
            if (next != null) {
                if (last.order() < next.order()) {
                    now.setNext(last).setNext(next);
                    break;
                }
                now = next;
                continue;
            }
            now.setNext(last);
            break;
        }
        return this;
    }

    @Override
    public TraverNetFilter getHeadFilter() {
        return wrapper;
    }

    private void submitExecuteTask(CompletableFuture future, ExecuteBean executeBean, Invocation invocation) {
        executorService.submit(() -> {
            wrapper.invoke(() -> {
                CompletableFuture<InvokeResult> irFuture = new CompletableFuture<>();
                try {
                    Method method = executeBean.getMethod();
                    Object o = method.invoke(executeBean.getSource(), invocation.getArgs());
                    if (method.getReturnType() == CompletableFuture.class) { // 异步方法
                        if (o == null) {
                            irFuture.complete(BasicInvokeResult.fail(
                                    new ProviderExecuteException(String.format("异步服务接口(%s)没有返回异步对象CompletableFuture，请检查服务代码", method.getName())),
                                    invocation.getAttachments()
                            ));
                        } else {
                            ((CompletableFuture<Object>) o).whenComplete((r, throwable) -> {
                                if (throwable != null) {
                                    if ((throwable instanceof CompletionException) && throwable.getCause() != null) {
                                        irFuture.complete(BasicInvokeResult.fail(new ProviderExecuteException(throwable.getCause()), invocation.getAttachments()));
                                    } else {
                                        irFuture.complete(BasicInvokeResult.fail(new ProviderExecuteException(throwable), invocation.getAttachments()));
                                    }
                                } else {
                                    irFuture.complete(BasicInvokeResult.success(r, invocation.getAttachments()));
                                }
                            });
                        }
                    } else {
                        irFuture.complete(BasicInvokeResult.success(o, invocation.getAttachments()));
                    }
                } catch (InvocationTargetException e) {
                    irFuture.complete(BasicInvokeResult.fail(new ProviderExecuteException(e.getTargetException()), invocation.getAttachments()));
                    log.error("反射执行目标方法异常", e.getTargetException());
                } catch (Exception e) {
                    log.error("执行目标方法发生未知异常", e);
                    irFuture.complete(BasicInvokeResult.fail(new ProviderExecuteException(e), invocation.getAttachments()));
                }
                return irFuture;
            }, invocation).whenComplete((invokeResult, throwable) -> {
                if (invokeResult.isSuccess()) {
                    future.complete(invokeResult.returnValue());
                } else {
                    future.completeExceptionally(invokeResult.cause());
                }
            });
        });
    }

    private boolean verifySameMethod(String methodName, Class[] argTypes, Method m2) {
        if (!StrUtil.equals(methodName, m2.getName())) {
            return false;
        }
        if ((argTypes == null ? 0 : argTypes.length) != m2.getParameterCount()) {
            return false;
        }
        if (m2.getParameterCount() == 0) {
            return true;
        }
        Class<?>[] m2ps = m2.getParameterTypes();
        for (int i = 0; i < argTypes.length; i++) {
            Class<?> m1p = argTypes[i];
            Class<?> m2p = m2ps[i];
            if (m1p != m2p) {
                return false;
            }
        }
        return true;
    }

}
