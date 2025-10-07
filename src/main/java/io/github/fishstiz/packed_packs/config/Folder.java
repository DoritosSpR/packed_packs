package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.lang.CollectionsUtil;
import net.minecraft.server.packs.repository.Pack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Folder implements Serializable {
    @SuppressWarnings("FieldCanBeLocal")
    private List<String> packIds = new ArrayList<>();

    public boolean trySetPacks(List<Pack> packs) {
        List<String> newPackIds = PackUtil.extractPackIds(packs);
        if (!CollectionsUtil.equalsOrdered(packIds, newPackIds)) {
            this.packIds = newPackIds;
            return true;
        }
        return false;
    }

    public List<String> getPackIds() {
        return List.copyOf(this.packIds);
    }
}
