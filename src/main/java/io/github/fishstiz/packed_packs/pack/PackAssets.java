package io.github.fishstiz.packed_packs.pack;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Folder;
import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.transform.interfaces.IPack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.ToastUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface PackAssets extends PackOptions {
    String ICON_FILENAME = "pack.png";
    String ZIP_PACK_EXTENSION = ".zip";
    ResourceLocation DEFAULT_FOLDER_ICON = ResourceUtil.getResource("textures/misc/unknown_folder.png");
    ResourceLocation DEFAULT_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");
    Component OPEN_FILE_TEXT = ResourceUtil.getText("file.open");
    Component OPEN_PARENT_TEXT = ResourceUtil.getText("file.parent.open");
    Component RENAME_FILE_TEXT = ResourceUtil.getText("file.rename");
    Component DELETE_FILE_TEXT = ResourceUtil.getText("file.delete");
    PackSource SOURCE = PackSource.create(name -> Component.translatable("pack.nameAndSource", name, ResourceUtil.getModName().withStyle(ChatFormatting.YELLOW))
            .withStyle(ChatFormatting.GRAY), false);

    void getOrLoadIcon(Pack pack, Consumer<ResourceLocation> iconCallback);

    boolean isResourcePacks();

    boolean isLocked();

    boolean isEnabled(Pack pack);

    Config.Packs getConfig();

    Folder getFolderConfig(@Nullable FolderPack folderPack);

    default boolean deletePack(Pack pack) {
        if (pack == null || this.isEnabled(pack)) {
            return false;
        }

        Path path = validatePackPath(pack);
        if (path == null) {
            showFailToast(getDeleteFailText(pack.getTitle().getString()));
            return false;
        }

        if (!PackUtil.deletePath(path)) {
            showFailToast(getDeleteFailText(PackUtil.fileName(path)));
            return false;
        }

        return true;
    }

    default boolean renamePack(Pack pack, String newName) {
        if (pack == null || this.isEnabled(pack)) {
            return false;
        }

        Path path = validatePackPath(pack);
        if (path == null) {
            showFailToast(getRenameFailText(pack.getTitle().getString(), newName));
            return false;
        }

        Path newPath = path.getParent().resolve(newName);
        if (!PackUtil.renamePath(path, newPath)) {
            showFailToast(getRenameFailText(PackUtil.fileName(path), PackUtil.fileName(newPath)));
            return false;
        }

        return true;
    }

    static @Nullable Path validatePackPath(Pack pack) {
        if (pack == null) {
            return null;
        }
        Path path = ((IPack) pack).packed_packs$getPath();
        if (path == null) {
            return null;
        }

        try {
            return Files.exists(path) ? path : null;
        } catch (SecurityException e) {
            PackedPacks.LOGGER.error("[packed_packs] Could not read file: '{}'", path);
            return null;
        }
    }

    static boolean isZipPack(Pack pack) {
        Path path = PackAssets.validatePackPath(pack);
        return path != null && Files.isRegularFile(path) && PackUtil.fileName(path).endsWith(ZIP_PACK_EXTENSION);
    }

    private static void showFailToast(Component message) {
        ToastUtil.onFileFailToast(message);
    }

    static ResourceLocation getDefaultIcon(Pack pack) {
        return pack instanceof FolderPack ? DEFAULT_FOLDER_ICON : DEFAULT_ICON;
    }

    static Component getRenameFailText(String from, String to) {
        return ResourceUtil.getText("file.rename.fail", from, to);
    }

    static Component getDeleteFailText(String fileName) {
        return ResourceUtil.getText("file.delete.fail", fileName);
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
                    IoSupplier<InputStream> iconIoSupplier = packResources.getRootResource(ICON_FILENAME);

                    if (iconIoSupplier == null) {
                        return getDefaultIcon(pack);
                    }

                    String id = pack.getId();
                    ResourceLocation resourceLocation = ResourceLocation.withDefaultNamespace(
                            "pack/" + Util.sanitizeName(id, ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(id) + "/icon"
                    );
                    InputStream iconStream = iconIoSupplier.get();

                    try {
                        NativeImage nativeImage = NativeImage.read(iconStream);
                        TextureManager manager = Minecraft.getInstance().getTextureManager();
                        Minecraft.getInstance().execute(() -> manager.register(resourceLocation, new DynamicTexture(nativeImage)));
                        packIcon = resourceLocation;
                    } catch (Throwable e) {
                        if (iconStream != null) {
                            try {
                                iconStream.close();
                            } catch (Throwable e2) {
                                e.addSuppressed(e2);
                            }
                        }
                        throw e;
                    }
                    if (iconStream != null) {
                        iconStream.close();
                    }
                }
                return packIcon;
            } catch (Exception e) {
                if (!(e instanceof NoSuchFileException)) {
                    PackedPacks.LOGGER.warn("Failed to load icon from pack '{}'", pack.getId(), e);
                }
                return getDefaultIcon(pack);
            }
        }, Util.backgroundExecutor());
    }
}
