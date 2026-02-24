package io.github.fishstiz.packed_packs.gui.components.events;

public sealed interface FileEvent extends PackListEvent permits
        FileDeleteEvent, // notification
        FileRenameOpenEvent, // action
        FileRenameCloseEvent, // action
        FileRenameEvent, // notif
        FolderOpenEvent, // action
        FolderCloseEvent { // action
    @Override
    default boolean pushToHistory() {
        return false;
    }
}
