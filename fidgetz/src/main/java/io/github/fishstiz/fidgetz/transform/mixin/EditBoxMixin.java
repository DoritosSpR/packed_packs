package io.github.fishstiz.fidgetz.transform.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static io.github.fishstiz.fidgetz.util.DrawUtil.renderScrollingStringLeftAlign;

@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget implements EditBoxAccess {
    @Unique
    private static final String fidgetz$SECTION_PLACEHOLDER = "fidgetz¶¶¶section¶¶¶placeholder";

    @Unique
    private boolean fidgetz$allowPastingSectionSign = false;

    protected EditBoxMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Shadow
    protected abstract boolean isEditable();

    @Shadow
    public abstract String getValue();

    @Unique
    private boolean fidgetz$shadow = true;

    @Override
    public boolean fidgetz$hasShadow() {
        return this.fidgetz$shadow;
    }

    @Override
    public void fidgetz$setShadow(boolean shadow) {
        this.fidgetz$shadow = shadow;
    }

    @Override
    public void fidgetz$allowPastingSectionSign(boolean allow) {
        this.fidgetz$allowPastingSectionSign = allow;
    }

    @WrapOperation(method = "renderWidget", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I",
            ordinal = 0
    ))
    public int drawScrollingString(GuiGraphics guiGraphics, Font font, FormattedCharSequence text, int x, int y, int color, Operation<Integer> original) {
        if ((EditBox) (Object) this instanceof ToggleableEditBox && !this.isEditable()) {
            return renderScrollingStringLeftAlign(
                    guiGraphics,
                    font,
                    Component.literal(this.getValue()),
                    this.getX(),
                    this.getY(),
                    this.getRight(),
                    this.getBottom(),
                    color,
                    this.fidgetz$hasShadow()
            );
        }

        return original.call(guiGraphics, font, text, x, y, color);
    }

    @WrapWithCondition(method = "renderWidget", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I",
            ordinal = 1
    ))
    public boolean isToggled(GuiGraphics instance, Font font, FormattedCharSequence text, int x, int y, int color) {
        return !((EditBox) (Object) this instanceof ToggleableEditBox) || this.isEditable();
    }

    @WrapOperation(method = "renderWidget", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"
    ))
    public int drawHintShadow(GuiGraphics instance, Font font, Component text, int x, int y, int color, Operation<Integer> original) {
        if (!((EditBox) (Object) this instanceof ToggleableEditBox)) {
            return original.call(instance, font, text, x, y, color);
        }
        return instance.drawString(font, text, x, y, color, this.fidgetz$hasShadow());
    }

    @ModifyArg(method = "insertText", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/StringUtil;filterText(Ljava/lang/String;)Ljava/lang/String;"
    ))
    private String placeholderSectionSigns(String text, @Share("isReplacedRef") LocalBooleanRef isReplacedRef) {
        if (!this.fidgetz$allowPastingSectionSign || !text.contains("§")) {
            return text;
        }

        isReplacedRef.set(true);
        return text.replaceAll("§", fidgetz$SECTION_PLACEHOLDER);
    }

    @ModifyExpressionValue(method = "insertText", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/StringUtil;filterText(Ljava/lang/String;)Ljava/lang/String;"
    ))
    private String replacePlaceholders(String original, @Share("isReplacedRef") LocalBooleanRef isReplacedRef) {
        if (!isReplacedRef.get()) {
            return original;
        }

        return original.replaceAll(fidgetz$SECTION_PLACEHOLDER, "§");
    }
}
