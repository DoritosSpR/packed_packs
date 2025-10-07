package io.github.fishstiz.packed_packs.transform.mixin.overrides;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.fishstiz.packed_packs.pack.PackOptionsResolver;
import io.github.fishstiz.packed_packs.transform.interfaces.ConfiguredPack;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Pack.class)
public abstract class PackMixin implements ConfiguredPack {
    @Unique
    @Nullable
    private PackOptionsResolver packed_packs$resolver;

    @Override
    public void packed_packs$setConfigurationResolver(PackOptionsResolver resolver) {
        this.packed_packs$resolver = resolver;
    }

    @Override
    public boolean packed_packs$isHidden() {
        return this.packed_packs$resolver != null && this.packed_packs$resolver.isHidden(self());
    }

    @Unique
    private Pack self() {
        return (Pack) (Object) this;
    }

    @WrapMethod(method = "isRequired")
    private boolean resolveRequired(Operation<Boolean> original) {
        PackOptionsResolver resolver = this.packed_packs$resolver;
        if (resolver != null && resolver.overridesRequired(self())) {
            return resolver.isRequired(self());
        }
        return original.call();
    }

    @WrapMethod(method = "isFixedPosition")
    private boolean resolveFixed(Operation<Boolean> original) {
        PackOptionsResolver resolver = this.packed_packs$resolver;
        if (resolver != null && resolver.overridesPosition(self())) {
            return resolver.isFixed(self());
        }
        return original.call();
    }

    @WrapMethod(method = "getDefaultPosition")
    private Pack.Position resolvePosition(Operation<Pack.Position> original) {
        if (this.packed_packs$resolver != null) {
            Pack.Position position = this.packed_packs$resolver.getPosition(self());
            if (position != null) {
                return position;
            }
        }
        return original.call();
    }

    @WrapMethod(method = "selectionConfig")
    private PackSelectionConfig resolveSelectionConfig(Operation<PackSelectionConfig> original) {
        if (this.packed_packs$resolver != null) {
            PackSelectionConfig selectionConfig = this.packed_packs$resolver.getSelectionConfig(self());
            if (selectionConfig != null) {
                return selectionConfig;
            }
        }
        return original.call();
    }
}
