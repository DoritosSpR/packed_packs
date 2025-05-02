package io.github.fishstiz.packed_packs.gui.components.events;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.packed_packs.gui.components.PackList;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;

public final class MoveEvent extends PackListEvent {
    public final ImmutableList<Pack> moved;

    public MoveEvent(PackList target, List<Pack> moved) {
        super(target);

        this.moved = ImmutableList.copyOf(moved);
    }

    @Override
    public boolean modifiesTarget() {
        return true;
    }

    public ImmutableList<Pack> moved() {
        return this.moved;
    }
}
