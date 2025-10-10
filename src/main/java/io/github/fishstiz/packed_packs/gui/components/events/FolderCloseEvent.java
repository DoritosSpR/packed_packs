package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.FolderPackList;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;

public record FolderCloseEvent(FolderPackList target, FolderPack folderPack) implements FileEvent {
}
