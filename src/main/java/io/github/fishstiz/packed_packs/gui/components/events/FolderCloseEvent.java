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

    public static FolderCloseEvent fromContextMenu(RequestContextMenuEvent event) {
        if (event.target() instanceof FolderPackList folderPackList && event.trigger() instanceof FolderPack folderPack) {
            return new FolderCloseEvent(folderPackList, folderPack);
        }
        throw new IllegalArgumentException("Context menu event not triggered from folder pack.");
    }

    public @Nullable FolderPack folderPack() {
        return this.folderPack;
    }

    @Override
    public boolean modifiesTarget() {
        return false;
    }
}
