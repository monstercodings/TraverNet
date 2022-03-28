package top.codings.travernet.common.bean;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DelayFuture<T> implements Delayed {
    private final AtomicLong sleepUntilTimeMs = new AtomicLong();
    @Getter
    @Setter
    private CompletableFuture<T> completableFuture;
    @Getter
    private final T data;

    public DelayFuture(long sleepMs) {
        this(sleepMs, null);
    }

    public DelayFuture(Duration duration) {
        this(duration.toMillis(), null);
    }

    public DelayFuture(long sleepMs, T data) {
        this.data = data;
        if (sleepMs <= 0) {
            throw new IllegalArgumentException("休眠时间必须大于0ms");
        }
        sleepUntilTimeMs.set(System.currentTimeMillis() + sleepMs);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(sleepUntilTimeMs.get() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o == this) {
            return 0;
        }
        long diff = getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
        return (diff < 0) ? -1 : ((diff > 0) ? 1 : 0);
    }
}
