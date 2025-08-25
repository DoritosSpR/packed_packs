package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;

public final class FileDeleteEvent extends PackListEvent {
    public FileDeleteEvent(PackList target) {
        super(target);
    }

    @Override
    public boolean pushToHistory() {
        return false;
    }
}
