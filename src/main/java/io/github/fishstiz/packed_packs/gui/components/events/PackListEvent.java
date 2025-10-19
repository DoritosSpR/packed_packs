package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;

public sealed interface PackListEvent permits
        BasicEvent,
        DragEvent,
        DropEvent,
        FileEvent,
        MoveEvent,
        RequestTransferEvent,
        SelectionEvent,
        PackAliasOpenEvent {
    PackList target();

    boolean pushToHistory();

    default String name() {
        return this.getClass().getSimpleName();
    }
}
