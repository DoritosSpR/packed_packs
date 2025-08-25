package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import net.minecraft.server.packs.repository.Pack;

public final class FileRenameOpenEvent extends PackListEvent {
    private final Pack trigger;

    public FileRenameOpenEvent(PackList target, Pack trigger) {
        super(target);
        this.trigger = trigger;
    }

    public Pack trigger() {
        return this.trigger;
    }

    @Override
    public boolean pushToHistory() {
        return false;
    }
}
