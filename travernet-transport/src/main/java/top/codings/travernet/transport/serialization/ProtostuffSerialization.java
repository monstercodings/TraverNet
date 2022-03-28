package top.codings.travernet.transport.serialization;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProtostuffSerialization implements Serialization {
    /**
     * 避免每次序列化都重新申请Buffer空间
     */
    private final static ThreadLocal<LinkedBuffer> BUFFER = ThreadLocal.withInitial(() -> LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
    /**
     * 缓存Schema
     */
    private final static Map<Class<?>, Schema<?>> SCHEMA_CACHE = new ConcurrentHashMap<>();

    @Override
    public <T> byte[] serialize(T obj) throws IOException {
        Class<T> clazz = (Class<T>) obj.getClass();
        Schema<T> schema = getSchema(clazz);
        byte[] data;
        try {
            data = ProtostuffIOUtil.toByteArray(obj, schema, BUFFER.get());
        } finally {
            BUFFER.get().clear();
        }

        return data;
    }

    @Override
    public <T> T deSerialize(byte[] data, Class<T> clz) throws IOException {
        Schema<T> schema = getSchema(clz);
        T obj = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(data, obj, schema);
        return obj;
    }

    private <T> Schema<T> getSchema(Class<T> clazz) {
        Schema<T> schema = (Schema<T>) SCHEMA_CACHE.get(clazz);
        if (schema == null) {
            //这个schema通过RuntimeSchema进行懒创建并缓存
            //所以可以一直调用RuntimeSchema.getSchema(),这个方法是线程安全的
            schema = RuntimeSchema.getSchema(clazz);
            if (schema != null) {
                SCHEMA_CACHE.put(clazz, schema);
            }
        }
        return schema;
    }
}
