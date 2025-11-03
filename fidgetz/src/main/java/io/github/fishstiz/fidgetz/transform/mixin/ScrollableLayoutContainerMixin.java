package io.github.fishstiz.fidgetz.transform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.fidgetz.transform.interfaces.UnpaddedScrollableLayout;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.Layout;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.gui.components.ScrollableLayout$Container")
public abstract class ScrollableLayoutContainerMixin {
    @Final
    @Shadow
    ScrollableLayout field_60720;

    @WrapOperation(method = "setX", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/Layout;setX(I)V"))
    private void removePaddingOnSet(Layout instance, int paddedX, Operation<Void> original, int x) {
        original.call(instance, ((UnpaddedScrollableLayout) field_60720).fidgetz$unpadded() ? x : paddedX);
    }
}
