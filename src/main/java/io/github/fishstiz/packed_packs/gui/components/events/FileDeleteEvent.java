package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;

public record FileDeleteEvent(PackListBase target) implements PackListEvent {
    @Override
    public boolean pushToHistory() {
        return false;
    }
}
