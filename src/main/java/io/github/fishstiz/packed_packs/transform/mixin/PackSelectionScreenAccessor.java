package io.github.fishstiz.packed_packs.transform.mixin;

import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PackSelectionScreen.class)
public interface PackSelectionScreenAccessor {
    @Accessor("model")
    PackSelectionModel getModel();

    @Invoker("reload")
    void invokeReload();

    @Invoker("closeWatcher")
    void invokeCloseWatcher();
}
