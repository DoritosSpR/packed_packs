package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;

public record BasicEvent(PackList target, boolean pushToHistory) implements PackListEvent {
    public BasicEvent(PackList target) {
        this(target, true);
    }
}
