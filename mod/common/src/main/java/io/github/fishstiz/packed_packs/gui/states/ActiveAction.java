package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;

public sealed interface ActiveAction {
    record RenamingPack(PackListIntent.OpenRename ctx) implements ActiveAction {
    }

    record EditingAliases(PackListIntent.OpenAliases ctx) implements ActiveAction {
    }

    record Dragging(PackListIntent.Drag ctx) implements ActiveAction {
    }
}