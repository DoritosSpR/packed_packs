package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;

public record SelectionEvent(PackList target, List<Pack> selected) implements PackListEvent {
    public SelectionEvent(PackList target) {
        this(target, target.copySelection());
    }

    @Override
    public boolean pushToHistory() {
        return true;
    }
}
