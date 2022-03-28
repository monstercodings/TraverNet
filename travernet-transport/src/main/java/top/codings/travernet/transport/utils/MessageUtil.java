package top.codings.travernet.transport.utils;

import top.codings.travernet.transport.protocol.Message;

public class MessageUtil {
    private final static ThreadLocal<Message> LOCAL_THREAD_CACHE = new InheritableThreadLocal<>();

    private MessageUtil() {

    }

    public static Message get() {
        return LOCAL_THREAD_CACHE.get();
    }

    public static void set(Message message) {
        LOCAL_THREAD_CACHE.set(message);
    }

    public static void clean() {
        LOCAL_THREAD_CACHE.remove();
    }
}
