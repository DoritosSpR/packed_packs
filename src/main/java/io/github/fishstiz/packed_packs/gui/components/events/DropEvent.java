package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;

public record DropEvent(PackListBase target, PackListBase destination, List<Pack> payload) implements PackListEvent {
    public DropEvent {
        payload = List.copyOf(payload);
    }

    @Override
    public boolean pushToHistory() {
        return true;
    }
}
