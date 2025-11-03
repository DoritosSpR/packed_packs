package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.packed_packs.compat.api.ModExtension;
import net.minecraft.resources.ResourceLocation;

public interface ModExtensionInternal extends ModExtension {
    ModContext mod();

    @Override
    default ResourceLocation id() {
        return this.mod().getInternalId();
    }
}
