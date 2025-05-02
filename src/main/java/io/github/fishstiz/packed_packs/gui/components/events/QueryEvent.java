package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.PackList;
import io.github.fishstiz.packed_packs.gui.components.Query;

public final class QueryEvent extends PackListEvent{
    private final Query query;

    public QueryEvent(PackList target) {
        super(target);

        this.query = target.copyQuery();
    }

    public Query query() {
        return this.query;
    }

    @Override
    public boolean modifiesTarget() {
        return true;
    }
}
