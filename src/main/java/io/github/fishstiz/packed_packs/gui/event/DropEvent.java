package io.github.fishstiz.packed_packs.gui.event;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.packed_packs.gui.components.list.PackList;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;

public final class DropEvent extends PackListEvent {
    private final PackList destination;
    private final ImmutableList<Pack> dropped;

    public DropEvent(PackList target, PackList destination, List<Pack> dropped) {
        super(target);

        this.destination = destination;
        this.dropped = ImmutableList.copyOf(dropped);
    }

    public PackList destination() {
        return this.destination;
    }

    public ImmutableList<Pack> dropped() {
        return this.dropped;
    }

    @Override
    public boolean modifiesTarget() {
        return true;
    }
}
