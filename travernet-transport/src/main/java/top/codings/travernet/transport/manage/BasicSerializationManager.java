package top.codings.travernet.transport.manage;

import top.codings.travernet.transport.bean.DefaultSerializationType;
import top.codings.travernet.transport.serialization.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BasicSerializationManager implements SerializationManager {
    private final Map<String, Serialization> serializationMap = new ConcurrentHashMap<>();

    @Override
    public SerializationManager register(String key, Serialization serialization) {
        serializationMap.put(key, serialization);
        return this;
    }

    @Override
    public Serialization get(String key) {
        return serializationMap.get(key);
    }

    public SerializationManager useDefault(Map<Class, String> classToKeyMap) {
        register(DefaultSerializationType.PROTOSTUFF.getValue(), new ProtostuffSerialization());
        register(DefaultSerializationType.HESSIAN.getValue(), new HessianSerialization());
        register(DefaultSerializationType.KRYO.getValue(), new KryoSerialization(classToKeyMap));
        register(DefaultSerializationType.FASTJSON.getValue(), new FastjsonSerialization());
        return this;
    }
}
