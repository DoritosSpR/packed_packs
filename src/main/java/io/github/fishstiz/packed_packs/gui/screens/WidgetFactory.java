package io.github.fishstiz.packed_packs.gui.screens;

import io.github.fishstiz.fidgetz.gui.components.ToggleableDialogContainer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenu;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.gui.components.pack.FileRenameModal;
import io.github.fishstiz.packed_packs.gui.components.pack.FolderDialog;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.gui.components.profile.ProfileList;
import io.github.fishstiz.packed_packs.gui.components.profile.Sidebar;
import io.github.fishstiz.packed_packs.gui.layouts.pack.AvailablePacksLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.CurrentPacksLayout;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.PackFileOperations;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.packed_packs.pack.PackRepositoryManager;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.SPACING;

/**
 * maybe hacky or overkill, but reduces first constructor call of {@link PackedPacksScreen} by ~25%.
 * some widgets should probably be created lazily like the dialogs, but that'll mess with the ordering,
 * the whole dialog system may need to be rewritten.
 */
public class WidgetFactory {
    private static boolean screenInitialized = false;
    private static boolean profilesInitialized = false;

    private WidgetFactory() {
    }

    private static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, Util.backgroundExecutor());
    }

    public static <S extends Screen & ToggleableDialogContainer & PackListEventListener> PackedPacksWidgets createWidgets(
            S screen,
            PackOptionsContext options,
            PackRepositoryManager repository,
            PackAssetManager assets
    ) {
        PackFileOperations fileOps = new PackFileOperations(options, repository);

        if (screenInitialized) {
            return new PackedPacksWidgets(
                    new AvailablePacksLayout(options, assets, fileOps, screen),
                    new CurrentPacksLayout(options, assets, fileOps, screen),
                    new FolderDialog(screen, options, assets, fileOps),
                    new FileRenameModal(screen, fileOps, assets),
                    ContextMenu.builder(screen)
                            .setSpacing(SPACING)
                            .setBackground(Theme.GRAY_800.getARGB())
                            .setBorderColor(Theme.GRAY_500.getARGB())
                            .build()
            );
        }

        var availablePacks = supplyAsync(() -> new AvailablePacksLayout(options, assets, fileOps, screen));
        var currentPacks = supplyAsync(() -> new CurrentPacksLayout(options, assets, fileOps, screen));
        var folderDialog = supplyAsync(() -> new FolderDialog(screen, options, assets, fileOps));
        var fileRenameModal = supplyAsync(() -> new FileRenameModal(screen, fileOps, assets));
        var contextMenu = supplyAsync(() -> ContextMenu.builder(screen)
                .setSpacing(SPACING)
                .setBackground(Theme.GRAY_800.getARGB())
                .setBorderColor(Theme.GRAY_500.getARGB())
                .build()
        );

        CompletableFuture.allOf(availablePacks, currentPacks, folderDialog, fileRenameModal, contextMenu).join();

        screenInitialized = true;

        return new PackedPacksWidgets(
                availablePacks.join(),
                currentPacks.join(),
                folderDialog.join(),
                fileRenameModal.join(),
                contextMenu.join()
        );
    }

    public static <S extends Screen & ToggleableDialogContainer> ProfileWidgets createProfileWidgets(
            S screen,
            Config.Packs userConfig,
            DevConfig.Packs config,
            BiConsumer<Profile, Profile> selectListener,
            Consumer<Profile> updateListener
    ) {
        if (profilesInitialized) {
            return new ProfileWidgets(new Sidebar(screen), new ProfileList(userConfig, config, selectListener, updateListener));
        }

        var sidebar = supplyAsync(() -> new Sidebar(screen));
        var profileList = supplyAsync(() -> new ProfileList(userConfig, config, selectListener, updateListener));

        CompletableFuture.allOf(sidebar, profileList).join();

        profilesInitialized = true;

        return new ProfileWidgets(sidebar.join(), profileList.join());
    }

    public record PackedPacksWidgets(
            AvailablePacksLayout availablePacksLayout,
            CurrentPacksLayout currentPacksLayout,
            FolderDialog folderDialog,
            FileRenameModal fileRenameModal,
            ContextMenu contextMenu
    ) {
        public List<PackList> packLists() {
            return List.of(this.availablePacksLayout.list(), this.currentPacksLayout().list(), this.folderDialog.root());
        }
    }

    public record ProfileWidgets(Sidebar sidebar, ProfileList profileList) {
    }
}
