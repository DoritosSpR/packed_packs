package io.github.fishstiz.packed_packs.transform.interfaces;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public interface IPack {
    default boolean packed_packs$nestedPack() {
        return false;
    }

    default void packed_packs$setNestedPack(boolean nested) {
    }

    default void packed_packs$setPath(Path path) {
    }

    default @Nullable Path packed_packs$getPath() {
        return null;
    }

    default void packed_packs$setAdditionalPrefix(@Nullable String additionalPrefix) {
    }

    default @NotNull String packed_packs$getAdditionalPrefix() {
        return "";
    }
}
