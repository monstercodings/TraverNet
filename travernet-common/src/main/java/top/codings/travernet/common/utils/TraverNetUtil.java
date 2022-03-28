package top.codings.travernet.common.utils;

import org.slf4j.MDC;

public final class TraverNetUtil {
    private final static String TRACE_ID_KEY = "traceId";

    private TraverNetUtil() {

    }

    public static String getCurrentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static void setCurrentTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    public static String removeCurrentTraceId() {
        String currentTraceId = getCurrentTraceId();
        MDC.remove(TRACE_ID_KEY);
        return currentTraceId;
    }
}
