package io.github.fishstiz.packed_packs.gui.event;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.packed_packs.gui.components.list.PackList;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;

public final class RequestTransferEvent extends PackListEvent {
    private final ImmutableList<Pack> payload;

    public RequestTransferEvent(PackList target, List<Pack> payload) {
        super(target);

        this.payload = ImmutableList.copyOf(payload);
    }

    @Override
    public boolean modifiesTarget() {
        return true;
    }

    public ImmutableList<Pack> payload() {
        return this.payload;
    }
}