package io.github.fishstiz.fidgetz.util.debounce;

import net.minecraft.Util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class PollingDebouncer<T> extends Debouncer<T> {
    private final AtomicReference<T> lastArg = new AtomicReference<>(null);
    private volatile boolean pending = false;
    private volatile long lastCallTime = 0;

    public PollingDebouncer(Consumer<T> task, long delay) {
        super(task, delay);
    }

    public PollingDebouncer(Runnable task, long delay) {
        super(task, delay);
    }

    @Override
    public void accept(T t) {
        this.lastCallTime = Util.getMillis();
        this.pending = true;
        this.lastArg.set(t);
    }

    public void abort() {
        this.pending = false;
    }

    public void poll() {
        if (this.pending && Util.getMillis() - this.lastCallTime >= this.delay) {
            try {
                this.task.accept(this.lastArg.get());
            } finally {
                this.pending = false;
            }
        }
    }
}