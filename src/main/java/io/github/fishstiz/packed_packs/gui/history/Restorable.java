package io.github.fishstiz.packed_packs.gui.history;

import org.jetbrains.annotations.NotNull;

public interface Restorable<T extends Restorable.Snapshot<T>> {
    String DEFAULT_EVENT_NAME = "Event";

    @NotNull T captureState(String eventName);

    default @NotNull T captureState() {
        return this.captureState(DEFAULT_EVENT_NAME);
    }

    void replaceState(@NotNull T snapshot);

    interface Snapshot<T extends Snapshot<T>> {
        Restorable<T> target();

        default String eventName() {
            return DEFAULT_EVENT_NAME;
        }

        @SuppressWarnings("unchecked")
        default void restore() {
            this.target().replaceState((T) this);
        }
    }
}
