package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;

// notification
public record FileDeleteEvent(PackList target) implements FileEvent {
}
