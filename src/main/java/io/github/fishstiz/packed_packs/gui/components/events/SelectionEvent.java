package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;

public record SelectionEvent(PackList target) implements PackListEvent {
    @Override
    public boolean pushToHistory() {
        return true;
    }
}
