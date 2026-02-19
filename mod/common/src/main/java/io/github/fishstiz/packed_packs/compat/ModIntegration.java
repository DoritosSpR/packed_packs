package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.packed_packs.api.PackedPacksInitializer;
import io.github.fishstiz.packed_packs.api.PreferenceRegistry;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public interface ModIntegration extends PackedPacksInitializer {
    ModContext mod();

    default @NotNull ResourceLocation id() {
        return id(this.mod());
    }

    static ResourceLocation id(ModContext mod) {
        return ResourceUtil.id(mod.getId());
    }

    static Component getWidgetPrefText(PreferenceRegistry.Key<?> key) {
        return ResourceUtil.getText("preferences.widgets." + key.id().getPath());
    }
}
