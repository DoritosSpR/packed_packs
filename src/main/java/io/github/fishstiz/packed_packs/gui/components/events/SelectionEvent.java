package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;

public record SelectionEvent(PackListBase target, List<Pack> selected) implements PackListEvent {
    public SelectionEvent(PackListBase target) {
        this(target, target.copySelection());
    }

    @Override
    public boolean pushToHistory() {
        return true;
    }
}
