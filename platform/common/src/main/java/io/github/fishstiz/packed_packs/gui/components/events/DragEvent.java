package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;

public record DragEvent(PackList target, List<Pack> payload, Pack trigger) implements PackListEvent {
    public DragEvent {
        if (payload.isEmpty()) {
            throw new IllegalStateException("Cannot create drag event with empty payload.");
        }

        payload = List.copyOf(payload);
    }

    @Override
    public boolean pushToHistory() {
        return false;
    }
}
