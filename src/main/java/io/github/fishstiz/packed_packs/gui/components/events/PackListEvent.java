package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;

public interface PackListEvent {
    PackListBase target();

    boolean pushToHistory();
}
