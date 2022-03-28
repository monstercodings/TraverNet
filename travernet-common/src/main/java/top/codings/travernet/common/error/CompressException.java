package top.codings.travernet.common.error;

public class CompressException extends TraverNetException {
    public CompressException() {
    }

    public CompressException(String message) {
        super(message);
    }

    public CompressException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompressException(Throwable cause) {
        super(cause);
    }

    public CompressException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
