package io.github.fishstiz.fidgetz.transform.interfaces;

public interface ITextRenderer {
    default void fidgetz$setShadow(boolean shadow) {
    }

    default boolean fidgetz$hasShadow() {
        return true;
    }
}
