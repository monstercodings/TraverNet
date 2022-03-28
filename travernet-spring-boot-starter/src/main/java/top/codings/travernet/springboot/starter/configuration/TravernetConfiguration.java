package top.codings.travernet.springboot.starter.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import top.codings.travernet.client.core.NodeClient;
import top.codings.travernet.client.factory.BasicRemoteProxyFactory;
import top.codings.travernet.client.factory.RemoteProxyFactory;

import java.util.Collection;
import java.util.Map;

@Getter
@Setter
@AutoConfigureAfter(NodeClient.class)
public class TravernetConfiguration {
    private final Map<String, Collection<String>> springBeanToTravernetServiceName;

    public TravernetConfiguration(Map<String, Collection<String>> springBeanToTravernetServiceName) {
        this.springBeanToTravernetServiceName = springBeanToTravernetServiceName;
    }

    @Bean
    @ConditionalOnBean(NodeClient.class)
    @ConditionalOnMissingBean(RemoteProxyFactory.class)
    RemoteProxyFactory remoteProxyFactory(NodeClient client) {
        return new BasicRemoteProxyFactory(client);
    }
}
