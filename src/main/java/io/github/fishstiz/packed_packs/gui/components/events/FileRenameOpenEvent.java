package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import net.minecraft.server.packs.repository.Pack;

public record FileRenameOpenEvent(PackList target, Pack trigger) implements PackListEvent {
    @Override
    public boolean pushToHistory() {
        return false;
    }
}
