package io.github.fishstiz.packed_packs.gui.history;

import org.jetbrains.annotations.NotNull;

public interface Restorable<T extends Restorable.Snapshot<T>> {
    @NotNull T captureState();

    void replaceState(@NotNull T snapshot);

    interface Snapshot<T extends Snapshot<T>> {
        Restorable<T> target();

        @SuppressWarnings("unchecked")
        default void restore() {
            this.target().replaceState((T) this);
        }
    }
}
