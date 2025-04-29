package io.github.fishstiz.packed_packs.transform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.packed_packs.gui.PackedPacksScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectionScreenMixin extends Screen {
    protected PackSelectionScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private LinearLayout fidgetz$footer;

    @Unique
    private int fidgetz$footerSpacing;

    @Unique
    private final Button packedPacks$button = Button.builder(Component.empty(), btn -> {
        if (this.minecraft == null) return;
        this.minecraft.setScreen(new PackedPacksScreen(this, this.minecraft.getResourcePackRepository()));
    }).size(20, 20).build();

    @WrapOperation(method = "init", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/LinearLayout;spacing(I)Lnet/minecraft/client/gui/layouts/LinearLayout;",
            ordinal = 1
    ))
    public LinearLayout setFooter(LinearLayout instance, int spacing, Operation<LinearLayout> original) {
        this.fidgetz$footerSpacing = spacing;
        this.fidgetz$footer = original.call(instance, spacing);
        this.addRenderableWidget(this.packedPacks$button);

        return this.fidgetz$footer;
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    public void addPackedPacksButton(CallbackInfo ci) {
        ScreenRectangle footerRect = fidgetz$footer.getRectangle();
        int x = footerRect.left() - this.packedPacks$button.getWidth() - this.fidgetz$footerSpacing;
        this.packedPacks$button.setPosition(x, footerRect.top());
    }
}
