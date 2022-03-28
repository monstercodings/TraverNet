package top.codings.travernet.registry.manage;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.client.core.NodeClient;
import top.codings.travernet.common.bean.DelayFuture;
import top.codings.travernet.common.delay.DelayManager;
import top.codings.travernet.common.error.NoProviderException;
import top.codings.travernet.model.node.bean.*;
import top.codings.travernet.registry.bean.NetNodeRecord;
import top.codings.travernet.transport.protocol.Message;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class BasicNetNodeManager implements NetNodeManager, RemoteExecuteManager {
    private final NetNode local;
    /**
     * 节点类型关联客户端
     */
    private final Map<NodeType, Map<String, NetNodeRecord>> typeNodeRecords = new ConcurrentHashMap<>();
    /**
     * 通道关联客户端
     */
    private final Map<Channel, NetNodeRecord> channelNetNodeRecordMap = new ConcurrentHashMap<>();
    /**
     * serviceName关联服务提供节点
     */
    private final Map<String, Collection<NetNodeRecord>> serviceNameToNode = new ConcurrentHashMap<>();
    private final DelayManager delayManager;

    @Setter
    private Consumer<String> changeServiceListFunction;

    public BasicNetNodeManager(NetNode local, DelayManager delayManager) {
        this.local = local;
        this.delayManager = delayManager;
    }

    @Override
    public void record(NetNode node, Channel channel) {
        NetNodeRecord record;
        if (channelNetNodeRecordMap.containsKey(channel)) { // 更新节点信息
            record = channelNetNodeRecordMap.get(channel);
            record.setNode(node);
            log.info("[TraverNet] {}节点{}({})信息更新", node.getNodeType().getDesc(), node.getName(), node.getId());
        } else { // 新增的节点
            channel.closeFuture().addListener(future -> {
                NetNodeRecord offline = channelNetNodeRecordMap.remove(channel);
                if (null == offline) {
                    return;
                }
                offline.offline();
                NetNode offlineNode = offline.getNode();
                log.info("[TraverNet] {}节点{}({})下线", offlineNode.getNodeType().getDesc(), offlineNode.getName(), offlineNode.getId());
                if (offlineNode.getNodeType() == NodeType.CONSUMER) {
                    typeNodeRecords.get(NodeType.CONSUMER).remove(offlineNode.getId());
                } else {
                    // 提供3分钟的默认保护时间
                    delayManager.delay(new DelayFuture<>(Duration.ofMinutes(3))).thenAccept(o -> {
                        if (offline.isOnline()) {
                            return;
                        }
                        synchronized (offline) {
                            if (offline.isOnline()) {
                                return;
                            }
                            typeNodeRecords.get(offlineNode.getNodeType()).remove(offlineNode.getId());
                        }
                        log.info("[TraverNet] {}节点{}({})已失效，移除该节点", offlineNode.getNodeType().getDesc(), offlineNode.getName(), offlineNode.getId());
                    });
                }
            });
            Map<String, NetNodeRecord> nodeRecordMap = typeNodeRecords.computeIfAbsent(node.getNodeType(), nodeType -> new ConcurrentHashMap<>());
            if (nodeRecordMap.containsKey(node.getId())) {
                record = nodeRecordMap.get(node.getId());
                synchronized (record) {
                    if (nodeRecordMap.containsKey(node.getId())) {
                        log.info("[TraverNet] {}节点{}({})重新连接成功", node.getNodeType().getDesc(), node.getName(), node.getId());
                        Channel oldChannel = record.getChannel();
                        // 为了保险起见再移除一次
                        channelNetNodeRecordMap.remove(oldChannel);
                        // 为了保险起见再关闭一次
                        oldChannel.close();
                        record.online(node, channel);
                    }
                }
            } else {
                log.info("[TraverNet] {}新节点{}({})注册上线", node.getNodeType().getDesc(), node.getName(), node.getId());
                record = new NetNodeRecord(node, channel);
                nodeRecordMap.put(node.getId(), record);
            }
            channelNetNodeRecordMap.put(channel, record);
        }
        // 记录服务能力清单
        boolean hasChange = recordServiceNames(record);
        if (hasChange) {
            if (local instanceof RegistryNode) {
                // 更新本节点信息
                if (log.isDebugEnabled()) {
                    log.debug("[TraverNet] {}节点{}({})的服务能力清单有变化,更新本节点信息", node.getNodeType().getDesc(), node.getName(), node.getId());
                }
                RegistryNode localRegistry = (RegistryNode) local;
                Map<String, Collection<RegistryNode.InnerNode>> serviceNameToRegistryIdsMap = localRegistry.getServiceNameToRegistryIdsMap();
                serviceNameToRegistryIdsMap.clear();
                serviceNameToNode.entrySet().forEach(e -> {
                    String serviceName = e.getKey();
                    Collection<NetNodeRecord> nodeRecords = e.getValue();
                    serviceNameToRegistryIdsMap.put(serviceName, nodeRecords.stream().map(netNodeRecord -> new RegistryNode.InnerNode(netNodeRecord.getNode().getId(), netNodeRecord.getNode().getNodeType())).collect(Collectors.toSet()));
                });
            }
            if (null != changeServiceListFunction) {
                changeServiceListFunction.accept(node.getId());
            }
        }
    }

    private boolean recordServiceNames(NetNodeRecord record) {
        // 检查服务能力清单是否有变化
        Collection<String> oldServiceNames = serviceNameToNode.keySet();
        NetNode node = record.getNode();
        boolean hasChange = false;
        switch (node.getNodeType()) {
            case PROVIDER:
                ProviderNode provider = (ProviderNode) node;
                hasChange = !isContainsAll(oldServiceNames, provider.getServiceNames());
                provider.getServiceNames()
                        .stream()
                        .forEach(serviceName -> updateRecordInfoForServiceName(record, serviceName));
                break;
            case REGISTRY:
                RegistryNode registry = (RegistryNode) node;
                hasChange = !isContainsAll(oldServiceNames, registry.getServiceNameToRegistryIdsMap().keySet());
                Map<String, Collection<RegistryNode.InnerNode>> serviceNameToRegistryIdsMap = registry.getServiceNameToRegistryIdsMap();
                serviceNameToRegistryIdsMap.entrySet().forEach(e -> updateRecordInfoForServiceName(record, e.getKey()));
                break;
        }
        return hasChange;
    }

    /**
     * 判断新服务列表是否在原服务列表范围内
     *
     * @param oldServiceNames
     * @param newServiceNames
     * @return
     */
    private boolean isContainsAll(Collection<String> oldServiceNames, Collection<String> newServiceNames) {
        return CollUtil.containsAll(oldServiceNames, newServiceNames);
    }

    private void updateRecordInfoForServiceName(NetNodeRecord record, String serviceName) {
        Collection<NetNodeRecord> netNodeRecords = serviceNameToNode
                .computeIfAbsent(serviceName, s -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        netNodeRecords.stream()
                .filter(netNodeRecord -> StrUtil.equals(netNodeRecord.getNode().getId(), record.getNode().getId()))
                .findFirst()
                .ifPresent(netNodeRecords::remove);
        netNodeRecords.add(record);
    }

    @Override
    public void offline(NetNode node) {
        Map<String, NetNodeRecord> idToNode = typeNodeRecords.get(node.getNodeType());
        if (null == idToNode) {
            return;
        }
        NetNodeRecord record = idToNode.remove(node.getId());
        if (null == record) {
            return;
        }
        record.getChannel().close();
    }

    @Override
    public void notifyClusters(String skipId, Collection<NodeClient> clusterClients) {
        Collection<NodeClient> removes = new HashSet<>();
        for (NodeClient client : clusterClients) {
            if (client.isStop()) {
                removes.add(client);
                continue;
            }
            if (StrUtil.equals(client.getTarget().getId(), skipId)) {
                continue;
            }
            client.send(client.getMessageCreator().createRequest(local));
        }
        if (removes.size() > 0) {
            clusterClients.removeAll(removes);
        }
    }

    @Override
    public CompletableFuture<NetNodeRecord> execute(Message<NodeRequest> message) {
        NodeRequest request = message.getContent();
        String serviceName = request.getServiceName();
        if (StrUtil.isBlank(serviceName)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("serviceName不能为空"));
        }
        Collection<NetNodeRecord> records = serviceNameToNode.getOrDefault(serviceName, new HashSet<>());
        // 优先查找本地服务者
        Collection<NetNodeRecord> providers = records.stream().filter(netNodeRecord -> netNodeRecord.getNode().getNodeType() == NodeType.PROVIDER).collect(Collectors.toSet());
        if (CollUtil.isNotEmpty(providers)) {
            records = providers;
        } else {
            // 找不到的话则过滤掉没有本地服务提供者的注册中心
            records = records.stream().filter(netNodeRecord -> {
                NetNode node = netNodeRecord.getNode();
                if (node.getNodeType() != NodeType.REGISTRY) {
                    return false;
                }
                RegistryNode registryNode = (RegistryNode) node;
                Map<String, Collection<RegistryNode.InnerNode>> serviceNameToRegistryIdsMap = registryNode.getServiceNameToRegistryIdsMap();
                Collection<RegistryNode.InnerNode> collection = serviceNameToRegistryIdsMap.get(serviceName);
                if (CollUtil.isEmpty(collection)) {
                    return false;
                }
                return collection.stream().anyMatch(innerNode -> innerNode.getNodeType() == NodeType.PROVIDER);
            }).collect(Collectors.toSet());
        }
        if (CollUtil.isNotEmpty(records)) {
            if (log.isTraceEnabled()) {
                log.trace("过滤之后剩余可转发的节点总计{}个", records.size());
            }
            CompletableFuture<NetNodeRecord> completableFuture = new CompletableFuture();
            tryToCallRemote(
                    message,
                    new LinkedList<>(CollUtil.toList(ArrayUtil.shuffle(records.toArray(NetNodeRecord[]::new)))),
                    completableFuture,
                    null);
            return completableFuture;
        }
        return CompletableFuture.failedFuture(new NoProviderException(String.format("没有提供者注册当前请求的服务(%s)", serviceName)));
    }

    private void tryToCallRemote(Message<NodeRequest> message, Queue<NetNodeRecord> queue, CompletableFuture<NetNodeRecord> completableFuture, Throwable throwable) {
        NetNodeRecord record = queue.poll();
        if (null == record) {
            completableFuture.completeExceptionally(throwable);
            return;
        }
        if (log.isTraceEnabled()) {
            NetNode node = record.getNode();
            log.trace("尝试将方法转发到{}({})执行", node.getNodeType().getDesc(), node.getId());
        }
        if (!record.isOnline()) {
            tryToCallRemote(message, queue, completableFuture, (throwable instanceof NoProviderException) ? throwable : new NoProviderException("服务提供者当前是离线状态，请稍后重试"));
            return;
        }
        record.getChannel().writeAndFlush(message).addListener((ChannelFutureListener) channelFuture -> {
            if (!channelFuture.isSuccess()) {
                tryToCallRemote(message, queue, completableFuture, channelFuture.cause());
                return;
            }
            completableFuture.complete(record);
            return;
        });
    }
}
