package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;
import net.minecraft.server.packs.repository.Pack;

public record FileRenameOpenEvent(PackListBase target, Pack trigger) implements PackListEvent {
    @Override
    public boolean pushToHistory() {
        return false;
    }
}
