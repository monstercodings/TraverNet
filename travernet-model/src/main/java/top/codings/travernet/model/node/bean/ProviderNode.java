package top.codings.travernet.model.node.bean;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import static top.codings.travernet.model.node.bean.NodeType.PROVIDER;

public class ProviderNode extends BasicNode {
    /**
     * 注册的服务接口id
     */
    private final Collection<String> serviceNames = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ProviderNode() {
        super(PROVIDER);
    }

    public ProviderNode(String id, String name) {
        super(id, name, PROVIDER);
    }

    public ProviderNode registry(String serviceName) {
        serviceNames.add(serviceName);
        return this;
    }

    public Collection<String> getServiceNames() {
        return Collections.unmodifiableCollection(serviceNames);
    }
}
