package top.codings.travernet.transport.bean;

import top.codings.travernet.common.delay.DelayManager;
import top.codings.travernet.model.node.bean.NetNode;
import top.codings.travernet.model.node.manage.ReqToRespManager;
import top.codings.travernet.transport.manage.CompressManager;
import top.codings.travernet.transport.manage.ContentTypeManager;
import top.codings.travernet.transport.manage.SerializationManager;

public interface NodeContext {
    NetNode getNetNode();

    SerializationManager getSerializationManager();

    ContentTypeManager getContentTypeManager();

    DelayManager getDelayManager();

    CompressManager getCompressManager();

    ReqToRespManager getReqToRespManager();

    MessageCreator getMessageCreator();

    boolean isStop();

    default void refresh() {
    }
}
