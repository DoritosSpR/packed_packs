package io.github.fishstiz.packed_packs.transform.mixin;

import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HeaderAndFooterLayout.class)
public interface HeaderAndFooterLayoutAccess {
    @Accessor("contentsFrame")
    FrameLayout getContentsFrame();
}
