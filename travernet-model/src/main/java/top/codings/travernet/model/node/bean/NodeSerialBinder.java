package top.codings.travernet.model.node.bean;

import lombok.Getter;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class NodeSerialBinder implements Delayed {
    @Getter
    private final NodeSerial request;
    @Getter
    private final CompletableFuture completableFuture;
    private final AtomicLong untilTime = new AtomicLong();
    private final AtomicBoolean handle = new AtomicBoolean(false);

    public NodeSerialBinder(NodeSerial request, CompletableFuture completableFuture, Duration duration) {
        this.request = request;
        this.completableFuture = completableFuture;
        untilTime.set(duration.toMillis() + System.currentTimeMillis());
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(untilTime.get() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o == this) {
            return 0;
        }
        long diff = getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
        return (diff < 0) ? -1 : ((diff > 0) ? 1 : 0);
    }

    public boolean canHandle() {
        return handle.compareAndSet(false, true);
    }

}
