package io.github.fishstiz.fidgetz.transform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.fidgetz.gui.components.ToggleableDialogContainer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @WrapOperation(method = "keyPressed", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;shouldCloseOnEsc()Z"
    ))
    public boolean shouldCloseDialogs(Screen instance, Operation<Boolean> original, KeyEvent keyEvent) {
        if (this instanceof ToggleableDialogContainer dialogContainer && keyEvent.isEscape()) {
            for (var dialog : dialogContainer.getOpenDialogs()) {
                if (dialog.shouldCloseOnEscape()) {
                    dialog.setOpen(false);
                    return false;
                }
            }
        }
        return original.call(instance);
    }
}
