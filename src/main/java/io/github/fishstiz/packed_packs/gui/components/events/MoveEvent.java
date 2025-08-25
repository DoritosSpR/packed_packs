package io.github.fishstiz.packed_packs.gui.components.events;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;

public final class MoveEvent extends PackListEvent {
    private final ImmutableList<Pack> moved;
    private final Pack trigger;

    public MoveEvent(PackList target, List<Pack> moved, Pack trigger) {
        super(target);

        this.moved = ImmutableList.copyOf(moved);
        this.trigger = trigger;
    }

    @Override
    public boolean pushToHistory() {
        return true;
    }

    public ImmutableList<Pack> moved() {
        return this.moved;
    }

    public Pack trigger() {
        return this.trigger;
    }
}
