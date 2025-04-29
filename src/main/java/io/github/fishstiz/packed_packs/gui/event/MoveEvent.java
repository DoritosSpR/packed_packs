package io.github.fishstiz.packed_packs.gui.event;

import io.github.fishstiz.packed_packs.gui.components.list.PackList;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class MoveEvent extends Event {
    public final @Unmodifiable List<Pack> moved;

    public MoveEvent(PackList target, @Unmodifiable List<Pack> moved) {
        super(target);

        this.moved = moved;
    }

    @Override
    public boolean modifiesTarget() {
        return true;
    }

    public @Unmodifiable List<Pack> movedSelection() {
        return this.moved;
    }
}
