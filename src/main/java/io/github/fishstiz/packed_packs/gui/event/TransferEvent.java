package io.github.fishstiz.packed_packs.gui.event;

import io.github.fishstiz.packed_packs.gui.components.list.PackList;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class TransferEvent extends Event {
    private final PackList destination;
    private final @Unmodifiable List<Pack> transferred;

    public TransferEvent(PackList target, PackList destination, @Unmodifiable List<Pack> transferred) {
        super(target);

        this.transferred = transferred;
        this.destination = destination;
    }

    @Override
    public boolean modifiesTarget() {
        return true;
    }

    public PackList destination() {
        return this.destination;
    }

    public @Unmodifiable List<Pack> transferredSelection() {
        return this.transferred;
    }
}