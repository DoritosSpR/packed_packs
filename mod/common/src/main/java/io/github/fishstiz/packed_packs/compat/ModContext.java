package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.resources.Identifier;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ModContext {
    String getId();

    boolean isLoaded();

    default Identifier getInternalId() {
        return ResourceUtil.id(this.getId());
    }

    private void logError(Throwable e) {
        PackedPacks.LOGGER.warn("[packed_packs] Error occurred while applying compatibility for mod '{}'", this.getId(), e);
    }

    default <T> T wrapError(Supplier<T> supplier, T defaultValue) {
        if (this.isLoaded()) {
            try {
                return supplier.get();
            } catch (LinkageError | Exception e) {
                this.logError(e);
            }
        }
        return defaultValue;
    }

    default <T, R> R wrapError(Function<T, R> mapper, R defaultValue, T value) {
        if (this.isLoaded()) {
            try {
                return mapper.apply(value);
            } catch (LinkageError | Exception e) {
                this.logError(e);
            }
        }
        return defaultValue;
    }

    default <T> void wrapError(T arg, Consumer<T> consumer) {
        if (this.isLoaded()) {
            try {
                consumer.accept(arg);
            } catch (LinkageError | Exception e) {
                this.logError(e);
            }
        }
    }

    default <T1, T2> void wrapError(T1 arg1, T2 arg2, BiConsumer<T1, T2> consumer) {
        if (this.isLoaded()) {
            try {
                consumer.accept(arg1, arg2);
            } catch (LinkageError | Exception e) {
                this.logError(e);
            }
        }
    }
}
