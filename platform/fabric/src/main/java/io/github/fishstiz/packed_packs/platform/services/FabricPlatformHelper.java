package io.github.fishstiz.packed_packs.platform.services;

import io.github.fishstiz.packed_packs.compat.api.ModExtension;
import io.github.fishstiz.packed_packs.config.FabricPreferences;
import io.github.fishstiz.packed_packs.config.Preferences;
import it.unimi.dsi.fastutil.objects.ReferenceImmutableList;
import net.fabricmc.fabric.impl.resource.loader.BuiltinModResourcePackSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.repository.Pack;

import java.nio.file.Path;
import java.util.List;

import static io.github.fishstiz.packed_packs.PackedPacks.MOD_ID;

public class FabricPlatformHelper implements PlatformHelper {
    @Override
    public String getPlatform() {
        return "fabric";
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    @Override
    public List<ModExtension> getExtensions() {
        return FabricLoader.getInstance().getEntrypoints(MOD_ID, ModExtension.class);
    }

    @Override
    public List<Preferences.Spec<?>> getPreferences() {
        return new ReferenceImmutableList<>(FabricPreferences.values());
    }

    @Override
    public boolean isBuiltInPack(Pack pack) {
        //noinspection UnstableApiUsage
        return pack.getPackSource() instanceof BuiltinModResourcePackSource;
    }
}
