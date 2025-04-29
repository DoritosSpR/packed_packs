package io.github.fishstiz.packed_packs.util;

public interface Restorable<T extends Restorable.Snapshot<T>> {
    T captureState();

    void replaceState(T snapshot);

    abstract class Snapshot<T extends Snapshot<T>> {
        protected final Restorable<T> target;

        protected Snapshot(Restorable<T> target) {
            this.target = target;
        }

        @SuppressWarnings("unchecked")
        public void restore() {
            this.target.replaceState((T) this);
        }
    }
}
