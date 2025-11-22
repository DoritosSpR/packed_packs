package io.github.fishstiz.fidgetz.transform.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.fishstiz.fidgetz.transform.interfaces.IStringWidget;
import net.minecraft.client.gui.components.StringWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StringWidget.class)
public class StringWidgetMixin implements IStringWidget {
    @Unique
    private int fidgetz$offsetY = 0;

    @Override
    public void fidgetz$setOffsetY(int offsetY) {
        this.fidgetz$offsetY = offsetY;
    }

    @ModifyExpressionValue(method = "visitLines", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/StringWidget;getY()I"
    ))
    private int applyOffsetOnRender(int original) {
        return original + fidgetz$offsetY;
    }
}
