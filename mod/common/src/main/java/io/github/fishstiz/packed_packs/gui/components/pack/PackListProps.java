package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.gui.components.actions.PackListAction;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.PackFileOperations;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;

import java.util.function.Consumer;

public record PackListProps(
        Consumer<PackListAction> dispatcher,
        ScreenContext screenContext,
        PackOptionsContext options,
        PackAssetManager assets,
        PackFileOperations fileOps
) {
}
