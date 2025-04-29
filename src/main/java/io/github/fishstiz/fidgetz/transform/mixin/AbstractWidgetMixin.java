package io.github.fishstiz.fidgetz.transform.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.fishstiz.fidgetz.transform.interfaces.ToggleableDialogContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
    @SuppressWarnings("ConstantConditions")
    @ModifyExpressionValue(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;containsPointInScissor(II)Z")
    )
    public boolean isNotOverlapped(
            boolean original,
            @Local(ordinal = 0, argsOnly = true) int mouseX,
            @Local(ordinal = 1, argsOnly = true) int mouseY
    ) {
        boolean isDialogOpen = Minecraft.getInstance().screen instanceof ToggleableDialogContainer container
                               && !container.fidgetz$getVisibleDialogs().isEmpty();

        return original && (!isDialogOpen || ((AbstractWidget) (Object) (this)).isMouseOver(mouseX, mouseY));
    }

    @SuppressWarnings({"DataFlowIssue", "ConstantConditions"})
    @WrapMethod(method = "isMouseOver")
    public boolean isNotOverlapped(double mouseX, double mouseY, Operation<Boolean> original) {
        boolean isMouseOver = original.call(mouseX, mouseY);
        boolean isOverlapped = false;

        if (Minecraft.getInstance().screen instanceof ToggleableDialogContainer container) {
            var self = (AbstractWidget) (Object) this;

            for (var dialog : container.fidgetz$getVisibleDialogs()) {
                if (dialog.isHovered()
                    && dialog.getRoot() != self
                    && !dialog.children().contains(self)
                    && dialog.isOverlapping(self)
                ) {
                    isOverlapped = true;
                    break;
                }
            }
        }

        return isMouseOver && !isOverlapped;
    }
}
