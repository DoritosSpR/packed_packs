package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;

public record FolderOpenEvent(PackListBase target, FolderPack opened) implements PackListEvent {
    @Override
    public boolean pushToHistory() {
        return false;
    }
}
