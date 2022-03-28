package top.codings.travernet.common.error;

public class TraverCryptoException extends TraverNetException {
    public TraverCryptoException() {
        super();
    }

    public TraverCryptoException(String message) {
        super(message);
    }

    public TraverCryptoException(String message, Throwable cause) {
        super(message, cause);
    }

    public TraverCryptoException(Throwable cause) {
        super(cause);
    }

    protected TraverCryptoException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
