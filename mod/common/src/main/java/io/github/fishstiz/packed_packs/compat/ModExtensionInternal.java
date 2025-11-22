package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.packed_packs.compat.api.ModExtension;
import net.minecraft.resources.Identifier;

public interface ModExtensionInternal extends ModExtension {
    ModContext mod();

    @Override
    default Identifier id() {
        return this.mod().getInternalId();
    }
}
