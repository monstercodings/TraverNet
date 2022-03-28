package top.codings.travernet.common.error;

public class NoConnectException extends RpcException {
    public NoConnectException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
