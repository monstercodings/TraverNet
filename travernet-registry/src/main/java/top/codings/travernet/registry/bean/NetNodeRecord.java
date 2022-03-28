package top.codings.travernet.registry.bean;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.codings.travernet.model.node.bean.NetNode;

import java.util.Date;

@Getter
@NoArgsConstructor
public class NetNodeRecord {
    @Setter
    private NetNode node;
    @Setter
    private Channel channel;
    private boolean online;
    private Date lastOfflineAt;

    public NetNodeRecord(NetNode node, Channel channel) {
        online = true;
        this.node = node;
        this.channel = channel;
    }

    public void offline() {
        online = false;
        lastOfflineAt = new Date();
    }

    public void online(NetNode node, Channel channel) {
        this.node = node;
        this.channel = channel;
        online = true;
    }
}
