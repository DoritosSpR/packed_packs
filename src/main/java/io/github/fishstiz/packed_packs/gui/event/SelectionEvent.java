package io.github.fishstiz.packed_packs.gui.event;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.packed_packs.gui.components.list.PackList;
import net.minecraft.server.packs.repository.Pack;

public final class SelectionEvent extends PackListEvent {
    private final ImmutableList<Pack> selected;

    public SelectionEvent(PackList target) {
        super(target);

        this.selected = target.getSelectionCopy();
    }

    @Override
    public boolean modifiesTarget() {
        return true;
    }

    public ImmutableList<Pack> selected() {
        return this.selected;
    }
}
