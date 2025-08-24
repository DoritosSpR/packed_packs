package io.github.fishstiz.packed_packs.transform.mixin.gui;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.gui.metadata.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.WorldDataConfiguration;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.nio.file.Path;
import java.util.function.Consumer;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    @Shadow
    protected abstract void tryApplyNewDataPacks(PackRepository packRepository, boolean shouldConfirm, Consumer<WorldDataConfiguration> consumer);

    @Shadow
    abstract void openDataPackSelectionScreen(WorldDataConfiguration worldDataConfiguration);

    @ModifyArg(method = "openDataPackSelectionScreen", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
    ))
    public @Nullable Screen replaceScreen(@Nullable Screen guiScreen, @Local(ordinal = 0) Pair<Path, PackRepository> pair) {
        if (!PackedPacks.CONFIG.getDatapacks().isReplaceOriginal()) return guiScreen;

        if (guiScreen instanceof PackSelectionScreen packScreen) {
            ((PackSelectionScreenAccessor) packScreen).invokeCloseWatcher();
        }

        return new PackedPacksScreen((CreateWorldScreen) (Object) this, new PackSelectionScreenArgs(
                pair.getSecond(),
                repository -> this.tryApplyNewDataPacks(repository, true, this::openDataPackSelectionScreen),
                pair.getFirst(),
                Component.translatable("dataPack.title")
        ));
    }
}
