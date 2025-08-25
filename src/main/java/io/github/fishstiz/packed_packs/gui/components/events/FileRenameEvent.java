package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;

public final class FileRenameEvent extends PackListEvent {
    private final Pack renamed;
    private final Component newName;

    public FileRenameEvent(PackList target, Pack renamed, Component newName) {
        super(target);

        this.renamed = renamed;
        this.newName = newName;
    }

    public Pack renamed() {
        return this.renamed;
    }

    public Component newName() {
        return this.newName;
    }

    @Override
    public boolean pushToHistory() {
        return false;
    }
}
