package io.github.fishstiz.packed_packs.transform.interfaces;

import io.github.fishstiz.packed_packs.pack.PackOptionsResolver;
import net.minecraft.server.packs.repository.Pack;

public interface ConfiguredPack {
    default void packed_packs$setConfigurationResolver(PackOptionsResolver resolver) {
    }

    default boolean packed_packs$isHidden() {
        return false;
    }

    Pack.Metadata packed_packs$getMetadata();
}
