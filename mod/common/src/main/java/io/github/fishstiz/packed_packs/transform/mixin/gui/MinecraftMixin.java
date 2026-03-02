package io.github.fishstiz.packed_packs.transform.mixin.gui;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.metadata.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.Executor;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin implements Executor {
    @Shadow
    @Nullable
    public Screen screen;

    @WrapMethod(method = "setScreen")
    private void replacePackScreen(Screen guiScreen, Operation<Void> original) {
        if (guiScreen instanceof PackSelectionScreen packScreen &&
            (((PackSelectionScreenAccessor) packScreen).packed_packs$getPrevious() == null) &&
            !(this.screen instanceof PackedPacksScreen)) {

            PackSelectionScreenArgs args = PackSelectionScreenArgs.extract(packScreen);

            if (Config.get().get(args.packType()).isReplaceOriginal()) {
                ((PackSelectionScreenAccessor) packScreen).invokeCloseWatcher();
                long section = Util.getNanos();
                guiScreen = new PackedPacksScreen(this.screen, args);
                PackedPacks.LOGGER.info("[packed_packs] SCREEN INIT V2 TOOK {}ms", (Util.getNanos() - section) / 1_000_000);
            }
        }
        original.call(guiScreen);
    }
}
