package top.codings.travernet.common.error;

public class ContentTypeException extends TraverNetException {
    public ContentTypeException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
