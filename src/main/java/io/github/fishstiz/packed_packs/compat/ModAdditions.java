package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.compat.api.ModExtension;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.util.lang.CollectionsUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static io.github.fishstiz.packed_packs.PackedPacks.LOGGER;
import static io.github.fishstiz.packed_packs.PackedPacks.MOD_ID;

public class ModAdditions {
    private static final List<ModExtension> EXTENSIONS;

    static {
        List<ModExtension> extensions = FabricLoader.getInstance().getEntrypoints(MOD_ID, ModExtension.class);
        EXTENSIONS = extensions.isEmpty()
                ? Collections.emptyList()
                : CollectionsUtil.topoSort(extensions, ModExtension::id, ModExtension::loadAfter);
    }

    private ModAdditions() {
    }

    public static void onCreateHeader(PackType packType, FlexLayout header, PackedPacksScreen screen, PackSelectionScreen original) {
        for (ModExtension ext : EXTENSIONS) {
            try {
                ext.onCreateHeader(packType, header, screen, original);
            } catch (Exception e) {
                LOGGER.error("[packed_packs] Error while creating header from extension {} ", ext.id(), e);
            }
        }
    }

    public static void onCreateEntry(PackType packType, PackList.Entry entry) {
        for (ModExtension ext : EXTENSIONS) {
            try {
                ext.onCreateEntry(packType, entry);
            } catch (Exception e) {
                LOGGER.error("[packed_packs] Error while creating entry for pack {} from extension {} ", entry.pack().getId(), ext.id(), e);
            }
        }
    }

    public static void onCreatePreferencesMenu(PackType packType, ContextMenuItemBuilder contextMenuItemBuilder) {
        for (ModExtension ext : EXTENSIONS) {
            try {
                ext.onCreatePreferencesMenu(packType, contextMenuItemBuilder);
            } catch (Exception e) {
                LOGGER.error("[packed_packs] Error while building context menu from extension {} ", ext.id(), e);
            }
        }
    }

    /**
     * @return mod id requesting reload
     */
    public static @Nullable String forceCommitOnClose(PackType packType) {
        for (ModExtension ext : EXTENSIONS) {
            try {
                if (ext.forceCommitOnClose(packType)) {
                    return ext.id().toString();
                }
            } catch (Exception e) {
                LOGGER.error("[packed_packs] Error while closing screen from extension {} ", ext.id(), e);
            }
        }
        return null;
    }

    public static boolean shouldIgnoreChange(PackType packType, Path path) {
        for (ModExtension ext : EXTENSIONS) {
            try {
                if (ext.shouldIgnoreChange(packType, path)) {
                    return true;
                }
            } catch (Exception e) {
                LOGGER.error("[packed_packs] Error while detecting file change from extension {} ", ext.id(), e);
            }
        }
        return false;
    }
}
