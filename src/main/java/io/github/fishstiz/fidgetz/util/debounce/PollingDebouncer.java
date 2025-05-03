package io.github.fishstiz.fidgetz.util.debounce;

import io.github.fishstiz.fidgetz.util.LogUtil;
import net.minecraft.Util;

import java.util.function.Consumer;

public class PollingDebouncer<T> extends Debouncer<T> {
    private boolean pending = false;
    private long lastCallTime = 0;
    private T lastArg = null;

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
        this.lastArg = t;
    }

    public void abort() {
        this.pending = false;
    }

    public void poll() {
        if (this.pending && Util.getMillis() - this.lastCallTime >= this.delay) {
            try {
                this.task.accept(this.lastArg);
            } catch (Exception e) {
                LogUtil.LOGGER.error("Exception in debounced task: ", e);
            } finally {
                this.pending = false;
            }
        }
    }
}