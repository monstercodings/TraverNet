package top.codings.travernet.common.error;

public class ConnectFailException extends RpcException {
    public ConnectFailException(String message) {
        super(message);
    }
}
