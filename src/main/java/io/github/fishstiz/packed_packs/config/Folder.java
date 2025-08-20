package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.server.packs.repository.Pack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Folder implements Serializable {
    private List<String> packIds = new ArrayList<>();

    public boolean setPacks(List<Pack> packs) {
        List<String> newPackIds = PackUtil.extractPackIds(packs);
        if (!this.packIds.equals(newPackIds)) {
            this.packIds =  newPackIds;
            return true;
        }
        return false;
    }

    public List<String> getPackIds() {
        return List.copyOf(this.packIds);
    }
}
