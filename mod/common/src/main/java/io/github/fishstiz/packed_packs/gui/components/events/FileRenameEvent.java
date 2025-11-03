package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;

public record FileRenameEvent(PackList target, Pack renamed, Component newName) implements FileEvent {
}
