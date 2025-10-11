package io.github.fishstiz.packed_packs.pack;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PackAssetManager {
    public static final ResourceLocation DEFAULT_FOLDER_ICON = ResourceUtil.getResource("textures/misc/unknown_folder.png");
    public static final ResourceLocation DEFAULT_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");
    private final Map<String, ResourceLocation> cachedIcons = new Object2ObjectOpenHashMap<>();
    private final Minecraft minecraft;
    private Map<String, ResourceLocation> staleIcons;

    public PackAssetManager(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void getOrLoadIcon(Pack pack, Consumer<ResourceLocation> iconCallback) {
        if (this.staleIcons != null) {
            ResourceLocation staleIcon = this.staleIcons.get(pack.getId());
            if (staleIcon != null) {
                iconCallback.accept(staleIcon);
            }
        }

        ResourceLocation cachedIcon = this.cachedIcons.get(pack.getId());
        if (cachedIcon != null) {
            iconCallback.accept(cachedIcon);
        } else {
            this.loadPackIcon(pack).thenAcceptAsync(location -> {
                this.cachedIcons.put(pack.getId(), location);
                iconCallback.accept(location);
            }, this.minecraft);
        }
    }

    public void clearIconCache() {
        this.staleIcons = new Object2ObjectOpenHashMap<>(this.cachedIcons);
        this.cachedIcons.clear();
    }

    public static ResourceLocation getDefaultIcon(Pack pack) {
        return pack instanceof FolderPack ? DEFAULT_FOLDER_ICON : DEFAULT_ICON;
    }

    /**
     * Copied from {@link PackSelectionScreen#loadPackIcon(TextureManager, Pack)}
     */
    private CompletableFuture<ResourceLocation> loadPackIcon(Pack pack) {
        return CompletableFuture.supplyAsync(() -> {
            try (PackResources packResources = pack.open()) {
                IoSupplier<InputStream> iconIoSupplier = packResources.getRootResource(PackUtil.ICON_FILENAME);
                if (iconIoSupplier == null) return getDefaultIcon(pack);

                ResourceLocation icon = ResourceLocation.withDefaultNamespace(hashIconName(pack.getId()));

                try (InputStream iconStream = iconIoSupplier.get()) {
                    NativeImage nativeImage = NativeImage.read(iconStream);
                    TextureManager manager = this.minecraft.getTextureManager();
                    this.minecraft.execute(() -> manager.register(icon, new DynamicTexture(nativeImage)));
                    return icon;
                }
            } catch (Exception e) {
                if (!(e instanceof NoSuchFileException)) {
                    PackedPacks.LOGGER.warn("Failed to load icon from pack '{}'", pack.getId(), e);
                }
                return getDefaultIcon(pack);
            }
        }, Util.backgroundExecutor());
    }

    private static String hashIconName(String id) {
        return "pack/" + Util.sanitizeName(id, ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(id) + "/icon";
    }
}
