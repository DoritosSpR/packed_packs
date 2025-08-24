package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;

public class FileOperationEvent extends PackListEvent {
    public FileOperationEvent(PackList target) {
        super(target);
    }

    @Override
    public boolean modifiesTarget() {
        return true;
    }
}
