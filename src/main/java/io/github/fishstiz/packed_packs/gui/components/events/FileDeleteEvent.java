package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;

public record FileDeleteEvent(PackList target) implements FileEvent {
}
