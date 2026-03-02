package io.github.fishstiz.packed_packs.gui.intents;

import io.github.fishstiz.packed_packs.gui.Intent;

public sealed interface ScreenIntent extends Intent {
    @Override
    default boolean record() {
        return false;
    }

    record SyncRepository() implements ScreenIntent {
    }

    record Commit() implements ScreenIntent {
    }
}
