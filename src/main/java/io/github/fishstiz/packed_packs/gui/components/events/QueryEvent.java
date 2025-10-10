package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;

public record QueryEvent(PackListBase target, Query query) implements PackListEvent{
    public QueryEvent(PackListBase target) {
        this(target, target.copyQuery());
    }

    @Override
    public boolean pushToHistory() {
        return true;
    }
}
