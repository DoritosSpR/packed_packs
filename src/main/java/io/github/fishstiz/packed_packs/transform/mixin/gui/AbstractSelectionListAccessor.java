package io.github.fishstiz.packed_packs.transform.mixin.gui;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSelectionList.class)
public interface AbstractSelectionListAccessor {
    @Accessor("scrolling")
    boolean packed_packs$scrolling();
}
