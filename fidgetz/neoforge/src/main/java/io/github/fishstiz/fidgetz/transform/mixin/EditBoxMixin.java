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
    @Shadow
    private boolean textShadow;

    public EditBoxMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Shadow
    public abstract void setTextShadow(boolean shadow);

    @Shadow
    protected abstract boolean isEditable();

    @Override
    public void fidgetz$setShadow(boolean shadow) {
        this.setTextShadow(shadow);
    }

    @Override
    public boolean fidgetz$hasShadow() {
        return this.textShadow;
    }

    @WrapOperation(method = "renderWidget", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I",
            ordinal = 0
    ))
    public int drawScrollingString(GuiGraphics instance, Font font, FormattedCharSequence text, int x, int y, int color, boolean dropShadow, Operation<Integer> original) {
        if ((EditBox) (Object) this instanceof ToggleableEditBox<?> toggleableEditBox && !this.isEditable()) {
            return renderScrollingStringLeftAlign(
                    instance,
                    font,
                    toggleableEditBox.getInactiveText(),
                    this.getX(),
                    this.getY(),
                    this.getRight(),
                    this.getBottom(),
                    color,
                    this.fidgetz$hasShadow()
            );
        }

        return original.call(instance, font, text, x, y, color, dropShadow);
    }

    @WrapWithCondition(method = "renderWidget", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I",
            ordinal = 1
    ))
    public boolean isToggled(GuiGraphics instance, Font font, FormattedCharSequence text, int x, int y, int color, boolean dropShadow) {
        return !((EditBox) (Object) this instanceof ToggleableEditBox) || this.isEditable();
    }
}
