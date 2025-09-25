package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.packed_packs.PackedPacks;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public enum Mod {
    RESOURCIFY("resourcify"),
    RESPACKOPTS("respackopts"),
    ETF("entity_texture_features"),
    VTD("vt_downloader");

    private final boolean loaded;
    private final String id;

    Mod(String id) {
        this.id = id;
        this.loaded = FabricLoader.getInstance().isModLoaded(id);
    }

    public String getId() {
        return this.id;
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    private void logError(Throwable e) {
        PackedPacks.LOGGER.warn("[packed_packs] Error occurred while applying compatibility for mod '{}'", this.getId(), e);
    }

    public <T> T wrapError(Supplier<T> supplier, T defaultValue) {
        try {
            if (this.isLoaded()) {
                return supplier.get();
            }
        } catch (LinkageError | Exception e) {
            this.logError(e);
        }
        return defaultValue;
    }

    public <T, R> R wrapError(Function<T, R> mapper, R defaultValue, T value) {
        try {
            if (this.isLoaded()) {
                return mapper.apply(value);
            }
        } catch (LinkageError | Exception e) {
            this.logError(e);
        }
        return defaultValue;
    }

    public <T> void wrapError(T arg, Consumer<T> consumer) {
        try {
            if (this.isLoaded()) {
                consumer.accept(arg);
            }
        } catch (LinkageError | Exception e) {
            this.logError(e);
        }
    }

    public <T1, T2> void wrapError(T1 arg1, T2 arg2, BiConsumer<T1, T2> consumer) {
        try {
            if (this.isLoaded()) {
                consumer.accept(arg1, arg2);
            }
        } catch (LinkageError | Exception e) {
            this.logError(e);
        }
    }

    public <T1, T2, T3> void wrapError(T1 arg1, T2 arg2, T3 arg3, TriConsumer<T1, T2, T3> consumer) {
        try {
            if (this.isLoaded()) {
                consumer.accept(arg1, arg2, arg3);
            }
        } catch (LinkageError | Exception e) {
            this.logError(e);
        }
    }
}
