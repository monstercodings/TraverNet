package top.codings.travernet.springboot.starter.factory;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.ClassUtils;
import top.codings.travernet.client.factory.RemoteProxyFactory;
import top.codings.travernet.common.annotation.TraverService;

import java.beans.Introspector;

public class TravernetFactoryBean<T> implements FactoryBean<T> {
    private final Log log = LogFactory.getLog(getClass());
    private final Class<T> clazz;
    private RemoteProxyFactory remoteProxyFactory;

    public TravernetFactoryBean(Class<T> clazz) {
        this.clazz = clazz;
    }

    public void setRemoteProxyFactory(RemoteProxyFactory remoteProxyFactory) {
        this.remoteProxyFactory = remoteProxyFactory;
    }

    @Override
    public T getObject() throws Exception {
        String id = clazz.getAnnotation(TraverService.class).value();
        if (StrUtil.isBlank(id)) {
            String shortClassName = ClassUtils.getShortName(clazz.getName());
            id = Introspector.decapitalize(shortClassName);
        }
        log.info(String.format("[TraverNet] 创建服务(%s)代理对象(%s)", clazz.getName(), id));
        T t = remoteProxyFactory.create(clazz);
        return t;
    }

    @Override
    public Class<?> getObjectType() {
        return clazz;
    }
}
