package io.github.fishstiz.packed_packs.gui.event;

import io.github.fishstiz.packed_packs.gui.components.list.PackList;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class SelectionEvent extends Event {
    private final @Unmodifiable List<Pack> selection;

    public SelectionEvent(PackList target) {
        super(target);

        this.selection = target.getSelectionCopy();
    }

    @Override
    public boolean modifiesTarget() {
        return true;
    }

    public List<Pack> selection() {
        return this.selection;
    }
}
