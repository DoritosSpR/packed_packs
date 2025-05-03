package io.github.fishstiz.fidgetz.transform.mixin;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
//    @SuppressWarnings("ConstantConditions")
//    @ModifyExpressionValue(method = "render", at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/client/gui/GuiGraphics;containsPointInScissor(II)Z")
//    )
//    public boolean isNotOverlapped(
//            boolean original,
//            @Local(ordinal = 0, argsOnly = true) int mouseX,
//            @Local(ordinal = 1, argsOnly = true) int mouseY
//    ) {
//        if (!(Minecraft.getInstance().screen instanceof ToggleableDialogScreen container)) {
//            return original;
//        }
//        if (container.getVisibleDialogs().isEmpty()) {
//            return original;
//        }
//        return original && ((AbstractWidget) (Object) (this)).isMouseOver(mouseX, mouseY);
//    }
//
//    @WrapMethod(method = "isMouseOver")
//    public boolean isNotOverlapped(double mouseX, double mouseY, Operation<Boolean> original) {
//        if (!original.call(mouseX, mouseY)) {
//            return false;
//        }
//        if (Minecraft.getInstance().screen instanceof ToggleableDialogScreen container) {
//            var self = (AbstractWidget) (Object) this;
//            for (var dialog : container.getVisibleDialogs()) {
//                if (dialog.isHovered()
//                    && dialog.getRoot() != self
//                    && !dialog.children().contains(self)
//                    && dialog.isOverlapping(self)) {
//                    return false;
//                }
//            }
//        }
//        return true;
//    }
}
