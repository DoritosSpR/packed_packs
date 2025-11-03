package io.github.fishstiz.packed_packs.compat.cursors_extended;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.transform.mixin.compat.cursors_extended.CursorTypeAccess;

public class CursorsExtended {
    public static final CursorType GRABBING = createOrDefault("grabbing", CursorTypes.RESIZE_ALL);

    private static CursorType createOrDefault(String name, CursorType fallback) {
        return Mod.CURSORS_EXTENDED.isLoaded()
                ? CursorTypeAccess.packed_packs$createCursorType(name, 0)
                : fallback;
    }

    private CursorsExtended() {
    }
}
