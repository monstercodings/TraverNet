package top.codings.travernet.client.factory;

public interface RemoteProxyFactory {
    <T> T create(Class<T> clazz);

    <T> T create(String id, Class<T> clazz);
}
