package top.codings.travernet.transport.manage;

import top.codings.travernet.transport.serialization.Serialization;

public interface SerializationManager {
    SerializationManager register(String key, Serialization serialization);

    Serialization get(String key);
}
