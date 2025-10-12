package io.github.fishstiz.packed_packs.gui.components;

import java.util.Collections;
import java.util.List;

public record SelectionContext<T>(List<T> selection, T item) {
    public SelectionContext {
        selection = Collections.unmodifiableList(selection);
    }

    public boolean isSelected() {
        return this.selection.contains(this.item);
    }

    public boolean isSelectedLast() {
        return !this.selection.isEmpty() && this.selection.getLast() == this.item;
    }

    public List<T> getItemOrSelection() {
        return this.isSelected() ? List.copyOf(this.selection) : List.of(this.item);
    }
}
