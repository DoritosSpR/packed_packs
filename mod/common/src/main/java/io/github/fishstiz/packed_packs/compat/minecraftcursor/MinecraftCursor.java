package io.github.fishstiz.packed_packs.compat.minecraftcursor;

import io.github.fishstiz.minecraftcursor.api.CursorController;
import io.github.fishstiz.minecraftcursor.api.CursorType;
import io.github.fishstiz.packed_packs.compat.Mod;

public final class MinecraftCursor {
    private MinecraftCursor() {
    }

    public static void handleDrag(boolean dragging) {
        if (!Mod.MINECRAFT_CURSOR.isLoaded()) return;
        Proxy.handleDrag(dragging);
    }

    private static final class Proxy {
        static void handleDrag(boolean dragging) {
            CursorController.getInstance().setSingleCycleCursor(dragging ? CursorType.GRABBING : CursorType.NOT_ALLOWED);
        }

        private Proxy() {
        }
    }
}
