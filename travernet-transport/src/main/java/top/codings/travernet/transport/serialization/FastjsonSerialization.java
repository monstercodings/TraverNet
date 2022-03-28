package top.codings.travernet.transport.serialization;

import com.alibaba.fastjson.JSON;

import java.io.IOException;

public class FastjsonSerialization implements Serialization {
    @Override
    public <T> byte[] serialize(T obj) throws IOException {
        return JSON.toJSONBytes(obj);
    }

    @Override
    public <T> T deSerialize(byte[] data, Class<T> clz) throws IOException {
        return JSON.parseObject(data, clz);
    }
}
