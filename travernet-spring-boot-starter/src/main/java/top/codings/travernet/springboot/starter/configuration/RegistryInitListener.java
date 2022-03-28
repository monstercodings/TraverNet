package top.codings.travernet.springboot.starter.configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import top.codings.travernet.common.error.TraverNetException;
import top.codings.travernet.model.node.bean.NetNode;
import top.codings.travernet.registry.core.NodeRegistry;

@ConditionalOnBean(NodeRegistry.class)
public class RegistryInitListener implements ApplicationListener<ContextRefreshedEvent> {
    private final Log log = LogFactory.getLog(getClass());
    private final NodeRegistry registry;

    public RegistryInitListener(NodeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 根容器为Spring容器
        if (event.getApplicationContext().getParent() == null) {
            NetNode netNode = registry.getNetNode();
            log.info(String.format("[TraverNet] %s已配置完成，开始启动(%s:%s)...", netNode.getNodeType().getDesc(), netNode.getHost(), netNode.getPort()));
            try {
                registry.start().get();
            } catch (Exception e) {
                throw new TraverNetException("travernet启动失败", e);
            }
            log.info(String.format("[TraverNet] %s启动完成(%s:%s)", netNode.getNodeType().getDesc(), netNode.getHost(), netNode.getPort()));
        }
    }
}
