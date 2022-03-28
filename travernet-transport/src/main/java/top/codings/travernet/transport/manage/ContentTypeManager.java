package top.codings.travernet.transport.manage;

import java.util.Collection;
import java.util.Map;

public interface ContentTypeManager {
    ContentTypeManager registry(byte key, Class contentType);

    Class get(byte key);

    byte get(Class clazz);

    Collection<Class> getAllContent();

    Map<Class, String> getClassToKeys();
}
