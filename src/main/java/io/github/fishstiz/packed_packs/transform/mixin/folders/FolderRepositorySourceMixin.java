package io.github.fishstiz.packed_packs.transform.mixin.folders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.pack.folder.FolderResources;
import io.github.fishstiz.packed_packs.transform.interfaces.IPack;
import io.github.fishstiz.packed_packs.pack.PackAssets;
import io.github.fishstiz.packed_packs.util.PackUtil;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(FolderRepositorySource.class)
public abstract class FolderRepositorySourceMixin {
    @Shadow
    @Final
    private PackSource packSource;

    @Shadow
    @Final
    private PackType packType;

    @Unique
    private static final ThreadLocal<Boolean> IS_SUBDIRECTORY = ThreadLocal.withInitial(() -> false);

    @Unique
    private static final ThreadLocal<Path> ADDITIONAL_PATH = new ThreadLocal<>();

    @Inject(method = "loadPacks", at = @At("RETURN"))
    private void ensureRemoveThreadLocals(Consumer<Pack> consumer, CallbackInfo ci) {
        IS_SUBDIRECTORY.remove();
        ADDITIONAL_PATH.remove();
    }

    @WrapOperation(method = "loadPacks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/FolderRepositorySource;discoverPacks(Ljava/nio/file/Path;Lnet/minecraft/world/level/validation/DirectoryValidator;Ljava/util/function/BiConsumer;)V"
    ))
    private void loadPacksFromAdditionalPaths(Path folder, DirectoryValidator validator, BiConsumer<Path, Pack.ResourcesSupplier> output, Operation<Void> original) {
        original.call(folder, validator, output);

        List<String> additionalFolders = PackedPacks.CONFIG.get(this.packType).getAdditionalFolders();
        if (!additionalFolders.isEmpty()) {
            Set<Path> seen = new ObjectOpenHashSet<>();
            seen.add(folder.toAbsolutePath().normalize());

            for (Path additionalFolder : PackUtil.mapValidDirectories(additionalFolders)) {
                try {
                    Path normalized = additionalFolder.toAbsolutePath().normalize();
                    ADDITIONAL_PATH.set(normalized);

                    if (seen.add(normalized)) {
                        original.call(additionalFolder, validator, output);
                    } else {
                        PackedPacks.LOGGER.warn("[packed_packs] Duplicate path found: '{}', ignoring", additionalFolder);
                    }
                } finally {
                    ADDITIONAL_PATH.remove();
                }
            }
        }
    }

    @WrapOperation(method = "discoverPacks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/FolderRepositorySource$FolderPackDetector;detectPackResources(Ljava/nio/file/Path;Ljava/util/List;)Ljava/lang/Object;"
    ))
    private static Object discoverNestedPacks(
            @Coerce PackDetector<Pack.ResourcesSupplier> instance,
            Path path,
            List<ForbiddenSymlinkInfo> list,
            Operation<Object> original,
            @Local(argsOnly = true) DirectoryValidator validator,
            @Local(argsOnly = true) BiConsumer<Path, Pack.ResourcesSupplier> output,
            @Share("suppressLog") LocalBooleanRef suppressLogRef
    ) {
        if (PackUtil.isNonPackDirectory(path)) {
            suppressLogRef.set(true);
            boolean isRoot = !IS_SUBDIRECTORY.get();
            try {
                if (isRoot) {
                    IS_SUBDIRECTORY.set(true);
                    discoverPacks(path, validator, output);
                }
            } catch (IOException e) {
                PackedPacks.LOGGER.warn("[packed_packs] Failed to list packs in {}", path, e);
            } finally {
                if (isRoot) {
                    IS_SUBDIRECTORY.remove();
                }
            }
        }

        return original.call(instance, path, list);
    }

    @WrapOperation(method = "discoverPacks", at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V",
            remap = false
    ))
    private static void suppressLogOnFolderDiscovery(Logger instance, String s, Object o, Operation<Void> original, @Share("suppressLog") LocalBooleanRef suppressLogRef) {
        if (!suppressLogRef.get() && !(o instanceof Path p && (p.endsWith(FolderResources.FOLDER_CONFIG_FILENAME) || p.endsWith(PackAssets.ICON_FILENAME)))) {
            original.call(instance, s, o);
        }
        suppressLogRef.set(false);
    }

    @ModifyArg(method = "method_45272", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/Pack;readMetaAndCreate(Lnet/minecraft/server/packs/PackLocationInfo;Lnet/minecraft/server/packs/repository/Pack$ResourcesSupplier;Lnet/minecraft/server/packs/PackType;Lnet/minecraft/server/packs/PackSelectionConfig;)Lnet/minecraft/server/packs/repository/Pack;"
    ))
    private PackLocationInfo modifyPackLocation(PackLocationInfo location, @Local(argsOnly = true) Path path) {
        Path additionalFolder = ADDITIONAL_PATH.get();

        if (IS_SUBDIRECTORY.get()) {
            return PackUtil.replicateLocationInfo(location, this.getPackSource(additionalFolder != null), PackUtil.generateNestedPackId(path));
        }
        if (additionalFolder != null) {
            return PackUtil.replicateLocationInfo(location, this.getPackSource(true), PackUtil.generatePackId(path));
        }

        return location;
    }

    @ModifyArg(method = "method_45272", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private Object bindDirToNestedPack(Object arg, @Local(argsOnly = true) Path path) {
        if (arg instanceof IPack pack) {
            pack.packed_packs$setNestedPack(IS_SUBDIRECTORY.get());
            pack.packed_packs$setPath(path);
        }
        return arg;
    }

    @Shadow
    public static void discoverPacks(Path folder, DirectoryValidator validator, BiConsumer<Path, Pack.ResourcesSupplier> output) throws IOException {
        throw new AssertionError();
    }

    @Unique
    private PackSource getPackSource(boolean externalPath) {
        return externalPath && this.packType == PackType.SERVER_DATA ? PackAssets.SOURCE : this.packSource;
    }
}
