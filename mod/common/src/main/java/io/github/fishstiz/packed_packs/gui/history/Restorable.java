package io.github.fishstiz.packed_packs.gui.history;

import org.jspecify.annotations.NonNull;

public interface Restorable<T extends Restorable.Snapshot<T>> {
    String DEFAULT_EVENT_NAME = "Event";

    @NonNull T captureState(String eventName);

    default @NonNull T captureState() {
        return this.captureState(DEFAULT_EVENT_NAME);
    }

    void replaceState(@NonNull T snapshot);

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
