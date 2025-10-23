package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.DevConfig;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The vanilla available packs map is an immutable copy ({@link com.google.common.collect.ImmutableMap}) of a {@link TreeMap}.
 */
public class PackAliasMap extends TreeMap<String, Pack> {
    private final DevConfig.Packs config;
    private Set<String> unresolvedIds;

    public PackAliasMap(DevConfig.Packs config, Map<String, Pack> map) {
        super(map);
        this.config = config;
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(key) || this.resolvePackId(key) != null;
    }

    /**
     * Called when rebuilding selected ids, so hopefully the resolved map is also built immediately.
     */
    @Override
    public Pack get(Object key) {
        Pack pack = super.get(key);
        if (pack != null) {
            return pack;
        }
        return this.resolvePackId(key);
    }

    private @Nullable Pack resolvePackId(Object key) {
        if (!(key instanceof String packId)) {
            return null;
        }
        if (this.unresolvedIds != null && this.unresolvedIds.contains(key)) {
            return null;
        }

        String resolvedPackId = this.config.getAndSaveCanonicalId(packId);
        if (resolvedPackId != null) {
            Pack resolvedPack = super.get(resolvedPackId);
            if (resolvedPack != null) {
                PackedPacks.LOGGER.info("[packed_packs] Resolved unknown pack '{}' to '{}'.", packId, resolvedPackId);
                this.put(resolvedPackId, resolvedPack);
            } else {
                PackedPacks.LOGGER.warn("[packed_packs] Unknown pack '{}' resolved to '{}', but no such pack is available.", packId, resolvedPackId);
                this.setUnresolved(packId);
            }
            return resolvedPack;
        }
        return null;
    }

    private void setUnresolved(String packId) {
        if (this.unresolvedIds == null) {
            this.unresolvedIds = new ObjectOpenHashSet<>();
        }
        this.unresolvedIds.add(packId);
    }
}
