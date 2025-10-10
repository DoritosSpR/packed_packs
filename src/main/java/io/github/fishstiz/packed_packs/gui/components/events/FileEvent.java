package io.github.fishstiz.packed_packs.gui.components.events;

public sealed interface FileEvent extends PackListEvent permits
        FileDeleteEvent,
        FileRenameOpenEvent,
        FileRenameCloseEvent,
        FileRenameEvent,
        FolderOpenEvent,
        FolderCloseEvent {
    @Override
    default boolean pushToHistory() {
        return false;
    }
}
