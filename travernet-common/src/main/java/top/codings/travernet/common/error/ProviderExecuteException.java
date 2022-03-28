package top.codings.travernet.common.error;

public class ProviderExecuteException extends TraverNetException {
    public ProviderExecuteException(String message) {
        super(message);
    }

    public ProviderExecuteException(Throwable cause) {
        super(cause);
    }

    protected ProviderExecuteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ProviderExecuteException(String message, Throwable cause) {
        super(message, cause);
    }
}
