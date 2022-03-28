package top.codings.travernet.client.bean;

import lombok.Getter;
import lombok.Setter;
import top.codings.travernet.transport.protocol.Message;

import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public class RetryMessage {
    private int retry;
    private Message message;
    private Throwable lastError;
    private CompletableFuture future = new CompletableFuture();

    public RetryMessage(Message message) {
        this.message = message;
    }
}
