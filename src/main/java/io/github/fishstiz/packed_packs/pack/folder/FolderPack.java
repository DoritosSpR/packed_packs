package io.github.fishstiz.packed_packs.pack.folder;

import io.github.fishstiz.packed_packs.config.ConfigLoader;
import io.github.fishstiz.packed_packs.config.Folder;
import io.github.fishstiz.packed_packs.transform.interfaces.IPack;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.lang.ObjectsUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class FolderPack extends Pack implements IPack {
    public static final Component FOLDER_OPEN_TEXT = ResourceUtil.getText("folder.open");
    public static final Component FOLDER_DESCRIPTION = ResourceUtil.getText("folder");
    public static final PackSource FOLDER_SOURCE = PackSource.create(
            name -> Component.translatable(
                            "pack.nameAndSource",
                            name,
                            ResourceUtil.getModName().withStyle(ChatFormatting.YELLOW)
                    )
                    .withStyle(ChatFormatting.GRAY),
            false
    );
    public static final PackSelectionConfig FOLDER_SELECTION_CONFIG = new PackSelectionConfig(false, Position.TOP, false);
    public static final Metadata FOLDER_METADATA = new Metadata(FOLDER_DESCRIPTION, PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), Collections.emptyList());
    private final Path path;
    private final String additionalPrefx;

    public FolderPack(String id, String name, String additionalPrefx, Path path) {
        super(
                new PackLocationInfo(id, Component.literal(name), FOLDER_SOURCE, Optional.empty()),
                new FolderResourcesSupplier(path),
                FOLDER_METADATA,
                FOLDER_SELECTION_CONFIG
        );
        this.path = path;
        this.additionalPrefx = additionalPrefx;
    }

    public CompletableFuture<Folder> loadConfig() {
        return CompletableFuture.supplyAsync(() -> {
            try (PackResources resources = this.open()) {
                var configIoSupplier = resources.getRootResource(FolderResources.FOLDER_CONFIG_FILENAME);
                if (configIoSupplier == null) {
                    throw new IOException();
                }
                try (InputStream inputStream = configIoSupplier.get()) {
                    return ConfigLoader.load(inputStream, Folder.class);
                }
            } catch(NoSuchFileException e) {
                return ObjectsUtil.peek(new Folder(), this::saveConfig);
            } catch (IOException e) {
                return new Folder();
            }
        });
    }

    public void saveConfig(Folder folder) {
        if (folder != null) {
            ConfigLoader.save(folder, this.path.resolve(FolderResources.FOLDER_CONFIG_FILENAME).toFile());
        }
    }

    @Override
    public @Nullable Path packed_packs$getPath() {
        return this.path;
    }

    @Override
    public @NotNull String packed_packs$getAdditionalPrefix() {
        return this.additionalPrefx;
    }
}
