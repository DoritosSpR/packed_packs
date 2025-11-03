package io.github.fishstiz.packed_packs.transform.mixin.compat.cursors_extended;

import com.mojang.blaze3d.platform.cursor.CursorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CursorType.class)
public interface CursorTypeAccess {
    @Invoker("<init>")
    static CursorType packed_packs$createCursorType(String name, long handle) {
        throw new AssertionError();
    }
}
