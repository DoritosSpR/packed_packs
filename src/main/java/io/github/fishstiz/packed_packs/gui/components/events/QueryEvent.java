package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;

public record QueryEvent(PackList target, Query query) implements PackListEvent{
    public QueryEvent(PackList target) {
        this(target, target.copyQuery());
    }

    @Override
    public boolean pushToHistory() {
        return true;
    }
}
