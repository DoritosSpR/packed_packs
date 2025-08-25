package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.FolderPackList;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import org.jetbrains.annotations.Nullable;

public final class FolderCloseEvent extends PackListEvent {
    private final FolderPack folderPack;

    public FolderCloseEvent(FolderPackList target, @Nullable FolderPack folderPack) {
        super(target);
        this.folderPack = folderPack;
    }

    public @Nullable FolderPack folderPack() {
        return this.folderPack;
    }

    @Override
    public boolean pushToHistory() {
        return false;
    }
}
