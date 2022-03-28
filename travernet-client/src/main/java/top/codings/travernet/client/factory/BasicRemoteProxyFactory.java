package top.codings.travernet.client.factory;

import cn.hutool.core.util.StrUtil;
import net.sf.cglib.proxy.Enhancer;
import top.codings.travernet.client.core.NodeClient;
import top.codings.travernet.common.annotation.TraverService;

public class BasicRemoteProxyFactory implements RemoteProxyFactory {
    private final NodeClient nodeClient;

    public BasicRemoteProxyFactory(NodeClient nodeClient) {
        this.nodeClient = nodeClient;
    }

    @Override
    public <T> T create(Class<T> clazz) {
        String id = clazz.getName();
        if (clazz.isAnnotationPresent(TraverService.class)) {
            id = clazz.getAnnotation(TraverService.class).value();
            if (StrUtil.isBlank(id)) {
                id = clazz.getName();
            }
        }
        return create(id, clazz);
    }

    @Override
    public <T> T create(String id, Class<T> clazz) {
        return (T) Enhancer.create(clazz, new RemoteMethodInterceptor(id, nodeClient));
    }
}
