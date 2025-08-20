package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.server.packs.repository.Pack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Folder implements Serializable {
    private List<String> packIds = new ArrayList<>();

    public void setPacks(List<Pack> packs) {
        this.packIds = PackUtil.extractPackIds(packs);
    }

    public List<String> getPackIds() {
        return List.copyOf(this.packIds);
    }
}
