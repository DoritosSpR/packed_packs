package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.pack.folder.FolderPack;

public record FolderState(
        FolderPack pack,
        PackListState contents
) {
}
