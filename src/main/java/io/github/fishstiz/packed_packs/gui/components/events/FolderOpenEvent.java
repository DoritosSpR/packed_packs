package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class FolderOpenEvent extends PackListEvent {
    private final FolderPack opened;

    public FolderOpenEvent(PackList target, FolderPack opened) {
        super(target);
        this.opened = Objects.requireNonNull(opened);
    }

    public static FolderOpenEvent fromContextMenu(RequestContextMenuEvent event) {
        if (event.trigger() instanceof FolderPack folderPack) {
            return new FolderOpenEvent(event.target(), folderPack);
        }
        throw new IllegalArgumentException("Context menu event not triggered from folder pack.");
    }

    public @NotNull FolderPack opened() {
        return this.opened;
    }

    @Override
    public boolean modifiesTarget() {
        return false;
    }
}
