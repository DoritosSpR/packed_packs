package io.github.fishstiz.packed_packs.util.pack;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.Config;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface PackAssets {
    ResourceLocation DEFAULT_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");

    void getOrLoadIcon(Pack pack, Consumer<ResourceLocation> iconCallback);

    boolean isResourcePacks();

    Path getDirectory();

    default Config.Packs getConfig() {
        return this.isResourcePacks() ? PackedPacks.CONFIG.getResourcepacks() : PackedPacks.CONFIG.getDatapacks();
    }

    /**
     * Copied from {@link PackSelectionScreen#loadPackIcon(TextureManager, Pack)}
     */
    @SuppressWarnings("all")
    static CompletableFuture<ResourceLocation> loadPackIcon(Pack pack) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ResourceLocation packIcon;
                try (PackResources packResources = pack.open()) {
                    IoSupplier<InputStream> ioSupplier = packResources.getRootResource("pack.png");

                    if (ioSupplier == null) {
                        return DEFAULT_ICON;
                    }

                    String id = pack.getId();
                    ResourceLocation resourceLocation = ResourceLocation.withDefaultNamespace(
                            "pack/" + Util.sanitizeName(id, ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(id) + "/icon"
                    );
                    InputStream inputStream = ioSupplier.get();

                    try {
                        NativeImage nativeImage = NativeImage.read(inputStream);
                        TextureManager manager = Minecraft.getInstance().getTextureManager();
                        Minecraft.getInstance().execute(() -> manager.register(resourceLocation, new DynamicTexture(nativeImage)));
                        packIcon = resourceLocation;
                    } catch (Throwable e) {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Throwable e2) {
                                e.addSuppressed(e2);
                            }
                        }
                        throw e;
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
                return packIcon;
            } catch (Exception e) {
                PackedPacks.LOGGER.warn("Failed to load icon from pack '{}'", pack.getId(), e);
                return DEFAULT_ICON;
            }
        });
    }
}
