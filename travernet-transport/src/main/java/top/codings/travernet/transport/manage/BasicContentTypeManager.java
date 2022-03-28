package top.codings.travernet.transport.manage;

import cn.hutool.core.util.StrUtil;
import top.codings.travernet.common.error.ContentTypeException;
import top.codings.travernet.model.node.bean.*;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BasicContentTypeManager implements ContentTypeManager {
    private final Map<String, Class> contentTypeMap = new ConcurrentHashMap<>();
    private final Map<Class, String> classToKeyMap = new ConcurrentHashMap<>();

    @Override
    public ContentTypeManager registry(byte key, Class contentType) {
        contentTypeMap.put(String.valueOf(key), contentType);
        classToKeyMap.put(contentType, String.valueOf(key));
        return this;
    }

    @Override
    public Class get(byte key) {
        return contentTypeMap.get(String.valueOf(key));
    }

    @Override
    public byte get(Class clazz) {
        String s = classToKeyMap.get(clazz);
        if (StrUtil.isBlank(s)) {
            throw new ContentTypeException(String.format("该内容类型(%s)尚未注册", clazz.getName()));
        }
        return Byte.parseByte(s);
    }

    @Override
    public Collection<Class> getAllContent() {
        return classToKeyMap.keySet();
    }

    @Override
    public Map<Class, String> getClassToKeys() {
        return classToKeyMap;
    }

    public ContentTypeManager useDefault() {
        registry((byte) 1, RegistryNode.class)
                .registry((byte) 2, ProviderNode.class)
                .registry((byte) 3, ConsumerNode.class)
                .registry((byte) 4, CloseCommand.class)
                .registry((byte) 10, NodeRequest.class)
                .registry((byte) 11, NodeResponse.class);
        return this;
    }
}
