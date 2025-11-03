package io.github.fishstiz.packed_packs.transform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;

@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow
    protected Minecraft minecraft;

    @WrapOperation(method = "updateResourcePacks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;"
    ))
    public CompletableFuture<Void> refreshPackedPacks(Minecraft instance, Operation<CompletableFuture<Void>> original) {
        if (this.minecraft != null && this.minecraft.screen instanceof PackedPacksScreen packedPacksScreen) {
            return original.call(instance).thenRunAsync(packedPacksScreen::revalidatePacks, this.minecraft);
        }
        return original.call(instance);
    }
}
