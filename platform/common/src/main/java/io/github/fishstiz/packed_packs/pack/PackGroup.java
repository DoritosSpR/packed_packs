package io.github.fishstiz.packed_packs.pack;

import com.google.common.collect.ImmutableList;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;

public record PackGroup(ImmutableList<Pack> selected, ImmutableList<Pack> unselected) {
    public static PackGroup of(List<Pack> selected, List<Pack> unselected) {
        return new PackGroup(ImmutableList.copyOf(selected), ImmutableList.copyOf(unselected));
    }
}