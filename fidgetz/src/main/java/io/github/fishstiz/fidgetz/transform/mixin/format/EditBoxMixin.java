package io.github.fishstiz.fidgetz.transform.mixin.format;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.github.fishstiz.fidgetz.transform.mixin.EditBoxAccess;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.regex.Pattern;

@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget implements EditBoxAccess {
    @Unique
    private static final String fidgetz$SECTION_PLACEHOLDER = Pattern.quote("fidgetz¶¶¶section¶¶¶placeholder" + Math.random());

    @Unique
    private boolean fidgetz$allowPastingSectionSign = false;

    protected EditBoxMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    public void fidgetz$allowPastingSectionSign(boolean allow) {
        this.fidgetz$allowPastingSectionSign = allow;
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
