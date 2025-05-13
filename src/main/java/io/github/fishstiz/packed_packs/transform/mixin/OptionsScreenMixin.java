package io.github.fishstiz.packed_packs.transform.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.gui.metadata.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    @Shadow
    protected abstract void applyPacks(PackRepository packRepository);

    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @WrapMethod(method = "method_47631")
    public Screen replaceScreen(Operation<Screen> original) {
        if (PackedPacks.CONFIG.getResourcepacks().isReplaceOriginal() && this.minecraft != null) {
            return new PackedPacksScreen((OptionsScreen) (Object) this, new PackSelectionScreenArgs(
                    this.minecraft.getResourcePackRepository(),
                    this::applyPacks,
                    this.minecraft.getResourcePackDirectory(),
                    Component.translatable("resourcePack.title")
            ));
        }
        return original.call();
    }
}
