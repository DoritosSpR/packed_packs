package io.github.fishstiz.packed_packs.gui.event;

import io.github.fishstiz.packed_packs.gui.components.list.PackList;

public abstract class PackListEvent {
    protected final PackList target;

    protected PackListEvent(PackList target) {
        this.target = target;
    }

    public final PackList target() {
        return this.target;
    }

    public abstract boolean modifiesTarget();
}
