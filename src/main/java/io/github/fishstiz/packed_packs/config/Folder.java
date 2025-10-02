package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.server.packs.repository.Pack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Folder implements Serializable {
    @SuppressWarnings("FieldCanBeLocal")
    private boolean locked = false;
    private List<String> packIds = new ArrayList<>();

    public boolean setPacks(List<Pack> packs) {
        if (!this.locked) {
            List<String> newPackIds = PackUtil.extractPackIds(packs);
            if (!this.packIds.equals(newPackIds)) {
                this.packIds = newPackIds;
                return true;
            }
        }
        return false;
    }

    public List<String> getPackIds() {
        return List.copyOf(this.packIds);
    }

    public boolean isLocked() {
        return this.locked;
    }
}
