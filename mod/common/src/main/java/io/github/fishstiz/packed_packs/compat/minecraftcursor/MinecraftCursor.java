package io.github.fishstiz.packed_packs.compat.minecraftcursor;

import io.github.fishstiz.minecraftcursor.api.CursorController;
import io.github.fishstiz.minecraftcursor.api.CursorType;
import io.github.fishstiz.packed_packs.compat.Mod;

import java.util.function.Consumer;

public class MinecraftCursor {
    private MinecraftCursor() {
    }

    private static final Consumer<Boolean> HANDLE_DRAG = dragging ->
            CursorController.getInstance().setSingleCycleCursor(dragging ? CursorType.GRABBING : CursorType.NOT_ALLOWED);

    public static void handleDrag(boolean dragging) {
        Mod.MINECRAFT_CURSOR.wrapError(dragging, HANDLE_DRAG);
    }
}
