package io.github.fishstiz.packed_packs.transform.mixin.folders;

import io.github.fishstiz.packed_packs.transform.interfaces.IPack;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.nio.file.Path;

@Mixin(Pack.class)
public abstract class PackMixin implements IPack {
    @Unique
    private boolean packed_packs$nested = false;

    @Unique
    private Path packed_packs$path;

    @Unique
    private String packed_packs$additionalPrefix = "";

    @Override
    public boolean packed_packs$nestedPack() {
        return this.packed_packs$nested;
    }

    @Override
    public void packed_packs$setNestedPack(boolean nested) {
        this.packed_packs$nested = nested;
    }

    @Override
    public void packed_packs$setPath(Path path) {
        this.packed_packs$path = path;
    }

    @Override
    public @Nullable Path packed_packs$getPath() {
        return this.packed_packs$path;
    }

    @Override
    public void packed_packs$setAdditionalPrefix(@Nullable String additionalPrefix) {
        if (additionalPrefix != null) {
            this.packed_packs$additionalPrefix = additionalPrefix;
        }
    }

    @Override
    public @NotNull String packed_packs$getAdditionalPrefix() {
        return this.packed_packs$additionalPrefix;
    }
}
