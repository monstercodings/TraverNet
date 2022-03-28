package top.codings.travernet.springboot.starter.configuration;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import top.codings.travernet.client.core.NodeClient;
import top.codings.travernet.common.factory.NodeBeanFactory;
import top.codings.travernet.model.node.bean.NetNode;
import top.codings.travernet.model.node.bean.NodeType;

import java.util.Collection;
import java.util.Map;

@ConditionalOnBean(NodeClient.class)
public class NodeClientInitListener implements ApplicationListener<ContextRefreshedEvent> {
    protected final Log log = LogFactory.getLog(getClass());
    private final NodeBeanFactory nodeBeanFactory;
    private final NodeClient nodeClient;
    private final TravernetConfiguration travernetConfiguration;

    public NodeClientInitListener(NodeBeanFactory nodeBeanFactory, NodeClient nodeClient, TravernetConfiguration travernetConfiguration) {
        this.nodeBeanFactory = nodeBeanFactory;
        this.nodeClient = nodeClient;
        this.travernetConfiguration = travernetConfiguration;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 根容器为Spring容器
        if (event.getApplicationContext().getParent() == null && nodeClient != null) {
            NetNode netNode = nodeClient.getNetNode();
            if (netNode.getNodeType() == NodeType.PROVIDER) {
                registryTravernetService(event.getApplicationContext());
                nodeClient.refresh();
            }
            /*NetNode target = nodeClient.getTarget();
            log.info(String.format("[TraverNet] %s已配置完成，启动连接注册中心(%s:%s)...", netNode.getNodeType().getDesc(), target.getHost(), target.getPort()));
            try {
                nodeClient.start().get();
            } catch (Exception e) {
                throw new TraverNetException("travernet启动失败", e);
            }
            log.info(String.format("[TraverNet] %s成功连接注册中心(%s:%s)", netNode.getNodeType().getDesc(), target.getHost(), target.getPort()));*/
        }
    }

    private void registryTravernetService(ApplicationContext context) {
        Map<String, Collection<String>> springBeanToTravernetServiceName = travernetConfiguration.getSpringBeanToTravernetServiceName();
        if (springBeanToTravernetServiceName.isEmpty()) {
            return;
        }
        log.info("[TraverNet] 开始注册RPC服务对象...");
        springBeanToTravernetServiceName.entrySet().forEach(e -> {
            Object bean = context.getBean(e.getKey());
            String ramAddr = bean.toString();
            if (StrUtil.isBlank(ramAddr)) {
                ramAddr = "未知内存地址";
            } else {
                if (ramAddr.contains("@")) {
                    ramAddr = ramAddr.substring(ramAddr.indexOf("@"));
                }
            }
            String finalRamAddr = ramAddr;
            e.getValue().forEach(s -> {
                log.info(String.format("[TraverNet] 注册服务提供者(%s) -> %s", s, finalRamAddr));
                nodeBeanFactory.registry(s, bean);
            });
        });
        log.info("[TraverNet] RPC服务对象已注册完成");
    }
}
