package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;

public record FileRenameEvent(PackListBase target, Pack renamed, Component newName) implements PackListEvent {
    @Override
    public boolean pushToHistory() {
        return false;
    }
}
