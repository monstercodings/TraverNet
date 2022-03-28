package top.codings.travernet.common.error;

/**
 * 根异常
 */
public class TraverNetException extends RuntimeException {
    public TraverNetException() {
        super();
    }

    public TraverNetException(String message) {
        super(message);
    }

    public TraverNetException(String message, Throwable cause) {
        super(message, cause);
    }

    public TraverNetException(Throwable cause) {
        super(cause);
    }

    protected TraverNetException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
