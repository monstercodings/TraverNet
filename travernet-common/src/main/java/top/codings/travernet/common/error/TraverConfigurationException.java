package top.codings.travernet.common.error;

public class TraverConfigurationException extends TraverNetException {
    public TraverConfigurationException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
