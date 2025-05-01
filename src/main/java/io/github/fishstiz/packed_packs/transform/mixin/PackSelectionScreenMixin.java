package io.github.fishstiz.packed_packs.transform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.packed_packs.gui.PackedPacksScreen;
import io.github.fishstiz.packed_packs.gui.metadata.GridWrapper;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.function.Consumer;

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectionScreenMixin extends Screen {
    protected PackSelectionScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private PackRepository packedPacks$repository;

    @Unique
    private FidgetzButton<GridWrapper<LinearLayout>> packedPacks$button;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void setRepository(PackRepository repository, Consumer<PackRepository> output, Path packDir, Component title, CallbackInfo ci) {
        this.packedPacks$repository = repository;
    }

    @WrapOperation(method = "init", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/LinearLayout;spacing(I)Lnet/minecraft/client/gui/layouts/LinearLayout;",
            ordinal = 1
    ))
    public LinearLayout addPackedPacksButton(LinearLayout instance, int spacing, Operation<LinearLayout> original) {
        if (this.minecraft == null) {
            return original.call(instance, spacing);
        }

        this.packedPacks$button = FidgetzButton.<GridWrapper<LinearLayout>>builder()
                .setDimensions(20, 20)
                .setOnPress(() -> this.minecraft.setScreen(new PackedPacksScreen(this, this.packedPacks$repository)))
                .setMetadata(new GridWrapper<>(original.call(instance, spacing), spacing))
                .build();

        this.addRenderableWidget(this.packedPacks$button);
        return this.packedPacks$button.getMetadata().layout();
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    public void repositionPackedPacksButton(CallbackInfo ci) {
        if (this.packedPacks$button != null) {
            GridWrapper<LinearLayout> layoutData = this.packedPacks$button.getMetadata();
            int x = layoutData.layout().getX() - this.packedPacks$button.getWidth() - layoutData.spacing();
            this.packedPacks$button.setPosition(x, layoutData.layout().getY());
        }
    }
}
