package top.codings.travernet.common.error;

public class SSLContextException extends TraverNetException {
    public SSLContextException() {
        super();
    }

    public SSLContextException(String message) {
        super(message);
    }

    public SSLContextException(String message, Throwable cause) {
        super(message, cause);
    }

    public SSLContextException(Throwable cause) {
        super(cause);
    }

    protected SSLContextException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
