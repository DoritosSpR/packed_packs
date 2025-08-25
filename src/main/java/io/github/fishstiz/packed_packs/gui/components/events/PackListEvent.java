package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;

public abstract class PackListEvent {
    protected final PackList target;

    protected PackListEvent(PackList target) {
        this.target = target;
    }

    public final PackList target() {
        return this.target;
    }

    public abstract boolean pushToHistory();
}
