package io.github.fishstiz.packed_packs.gui.event;

import io.github.fishstiz.packed_packs.gui.components.list.PackList;

public abstract class Event {
    protected final PackList target;

    protected Event(PackList target) {
        this.target = target;
    }

    public final PackList target() {
        return this.target;
    }

    public abstract boolean modifiesTarget();
}
