package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;

public sealed interface PackListEvent extends Event permits
        DragEvent, // action
        DropEvent, // notif
        FileEvent,
        MoveEvent, // ??
        RequestTransferEvent, // action
        SelectionEvent, // to focus ?
        PackAliasOpenEvent {
    PackList target();
}
