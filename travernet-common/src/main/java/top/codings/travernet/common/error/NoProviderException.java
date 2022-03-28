package top.codings.travernet.common.error;

public class NoProviderException extends RpcException {
    public NoProviderException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
