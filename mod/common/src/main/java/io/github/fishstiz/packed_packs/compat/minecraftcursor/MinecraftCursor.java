package io.github.fishstiz.packed_packs.compat.minecraftcursor;

import io.github.fishstiz.minecraftcursor.api.CursorController;
import io.github.fishstiz.minecraftcursor.api.CursorType;
import io.github.fishstiz.packed_packs.compat.Mod;

import java.util.function.Consumer;

public final class MinecraftCursor {
    private MinecraftCursor() {
    }

    private static final Consumer<Boolean> HANDLE_DRAG = dragging ->
            CursorController.getInstance().setSingleCycleCursor(dragging ? CursorType.GRABBING : CursorType.NOT_ALLOWED);

    public static void handleDrag(boolean dragging) {
        if (!Mod.MINECRAFT_CURSOR.isLoaded()) return;

        HANDLE_DRAG.accept(dragging);
    }
}
