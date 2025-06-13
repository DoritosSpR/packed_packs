package io.github.fishstiz.fidgetz.transform.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import static io.github.fishstiz.fidgetz.util.DrawUtil.renderScrollingStringLeftAlign;

@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget implements EditBoxAccess {
    protected EditBoxMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Shadow
    protected abstract boolean isEditable();

    @Shadow
    public abstract String getValue();

    @WrapOperation(method = "renderWidget", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
            ordinal = 0
    ))
    public void drawScrollingString(GuiGraphics guiGraphics, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow, Operation<Integer> original) {
        if ((EditBox) (Object) this instanceof ToggleableEditBox && !this.isEditable()) {
            renderScrollingStringLeftAlign(
                    guiGraphics,
                    font,
                    Component.literal(this.getValue()),
                    this.getX(),
                    this.getY(),
                    this.getRight(),
                    this.getBottom(),
                    color,
                    shadow
            );
            return;
        }

        original.call(guiGraphics, font, text, x, y, color, shadow);
    }

    @WrapWithCondition(method = "renderWidget", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
            ordinal = 1
    ))
    public boolean isToggled(GuiGraphics instance, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        return !((EditBox) (Object) this instanceof ToggleableEditBox) || this.isEditable();
    }
}
