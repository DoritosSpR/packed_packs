package io.github.fishstiz.packed_packs.platform.services;

import io.github.fishstiz.packed_packs.compat.api.ModExtension;
import io.github.fishstiz.packed_packs.config.Preferences;
import net.minecraft.server.packs.repository.Pack;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public interface PlatformHelper {
    String getPlatform();

    Path getConfigDir();

    boolean isModLoaded(String id);

    default List<ModExtension> getExtensions() {
        return Collections.emptyList();
    }

    default List<Preferences.Spec<?>> getPreferences() {
        return Collections.emptyList();
    }

    default boolean isBuiltInPack(Pack pack) {
        return false;
    }
}
