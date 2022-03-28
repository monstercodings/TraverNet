package top.codings.travernet.transport.manage;

import top.codings.travernet.transport.compress.Compress;

public interface CompressManager {
    CompressManager registry(String key, Compress compress);

    Compress get(String key);
}
