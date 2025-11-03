package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;

public record MoveEvent(PackList target, Pack trigger, List<Pack> payload) implements PackListEvent {
    public MoveEvent {
        payload = List.copyOf(payload);
    }

    @Override
    public boolean pushToHistory() {
        return true;
    }
}
