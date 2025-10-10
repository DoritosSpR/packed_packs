package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;

public record BasicEvent(PackListBase target, boolean pushToHistory) implements PackListEvent {
    public BasicEvent(boolean pushToHistory) {
        this(null, pushToHistory);
    }
}
