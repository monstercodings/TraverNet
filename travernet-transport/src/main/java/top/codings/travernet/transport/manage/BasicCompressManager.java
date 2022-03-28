package top.codings.travernet.transport.manage;

import top.codings.travernet.transport.compress.Compress;
import top.codings.travernet.transport.compress.EmptyCompress;
import top.codings.travernet.transport.compress.SnappyCompress;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BasicCompressManager implements CompressManager {
    private final Map<String, Compress> compressMap = new ConcurrentHashMap<>();

    @Override
    public CompressManager registry(String key, Compress compress) {
        compressMap.put(key, compress);
        return this;
    }

    @Override
    public Compress get(String key) {
        return compressMap.get(key);
    }

    public CompressManager useDefault() {
        registry("00", new EmptyCompress())
                .registry("01", new SnappyCompress());
        return this;
    }
}
