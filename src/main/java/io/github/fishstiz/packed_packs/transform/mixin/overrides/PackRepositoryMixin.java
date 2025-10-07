package io.github.fishstiz.packed_packs.transform.mixin.overrides;

import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.pack.PackOptionsResolver;
import io.github.fishstiz.packed_packs.transform.interfaces.ConfiguredPack;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(value = PackRepository.class, priority = 5000)
public abstract class PackRepositoryMixin {
    @Unique
    private PackOptionsResolver packed_packs$resolver;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void setConfigOnInit(RepositorySource[] sources, CallbackInfo ci) {
        if (sources.length > 0) {
            if (sources[0] instanceof ClientPackSource) {
                this.packed_packs$resolver = new PackOptionsResolver(PackedPacks.CONFIG.getResourcepacks());
            }
            if (sources[0] instanceof ServerPacksSource) {
                this.packed_packs$resolver = new PackOptionsResolver(PackedPacks.CONFIG.getDatapacks());
            }
        }
    }

    @ModifyArg(method = "discoverAvailable", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/RepositorySource;loadPacks(Ljava/util/function/Consumer;)V"
    ))
    private Consumer<Pack> applyResolverOnLoad(Consumer<Pack> onLoad) {
        if (this.packed_packs$resolver == null) {
            return onLoad;
        }

        return pack -> {
            ((ConfiguredPack) pack).packed_packs$setConfigurationResolver(this.packed_packs$resolver);
            onLoad.accept(pack);
        };
    }
}
