package io.github.fishstiz.packed_packs.gui.screens;

import io.github.fishstiz.fidgetz.gui.components.Modal;
import io.github.fishstiz.fidgetz.gui.components.ToggleableDialog;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenu;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.components.pack.FileRenameModal;
import io.github.fishstiz.packed_packs.gui.components.pack.FolderDialog;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.gui.components.pack.PackListProps;
import io.github.fishstiz.packed_packs.gui.layouts.OptionsLayout;
import io.github.fishstiz.packed_packs.gui.layouts.ProfilesLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.AvailablePacksLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.CurrentPacksLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.PackAliasLayout;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.PackFileOperations;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.packed_packs.pack.PackRepositoryManager;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.SPACING;

/**
 * maybe hacky or overkill, but reduces first constructor call of {@link PackedPacksScreen} by ~25%.
 */
record Components(
        PackFileOperations fileOps,
        ProfilesLayout profilesLayout,
        AvailablePacksLayout availablePacks,
        CurrentPacksLayout currentPacks,
        FolderDialog folderDialog,
        FileRenameModal renameModal,
        ContextMenu contextMenu,
        Modal<OptionsLayout> optionsModal,
        @Nullable Modal<PackAliasLayout> aliasModal
) {
    private static boolean warmed = false;

    private static <T> CompletableFuture<T> async(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, Util.backgroundExecutor());
    }

    static Components bootstrap(
            PackedPacksScreen screen,
            PackOptionsContext options,
            PackRepositoryManager repository,
            PackAssetManager assets,
            IntSupplier maxHeight
    ) {
        PackFileOperations fileOps = new PackFileOperations(options, repository);
        PackListProps props = new PackListProps(screen, screen.ctx(), options, assets, fileOps);

        if (warmed) {
            return new Components(
                    fileOps,
                    new ProfilesLayout(screen, screen, options),
                    new AvailablePacksLayout(props),
                    new CurrentPacksLayout(props),
                    new FolderDialog(screen, props),
                    new FileRenameModal(screen, assets),
                    buildMenu(screen),
                    buildOptionsModal(screen, maxHeight, options),
                    buildAliasModal(screen, options, assets)
            );
        }

        var profiles = async(() -> new ProfilesLayout(screen, screen, options));
        var available = async(() -> new AvailablePacksLayout(props));
        var current = async(() -> new CurrentPacksLayout(props));
        var folder = async(() -> new FolderDialog(screen, props));
        var rename = async(() -> new FileRenameModal(screen, assets));
        var menu = async(() -> buildMenu(screen));
        var alias = async(() -> buildAliasModal(screen, options, assets));
        var optionsModal = async(() -> buildOptionsModal(screen, maxHeight, options));

        CompletableFuture.allOf(available, current, folder, rename, menu, alias, profiles).join();

        warmed = true;

        return new Components(
                fileOps,
                profiles.join(),
                available.join(),
                current.join(),
                folder.join(),
                rename.join(),
                menu.join(),
                optionsModal.join(),
                alias.join()
        );
    }

    private static ContextMenu buildMenu(PackedPacksScreen screen) {
        return ContextMenu.builder(screen)
                .setSpacing(SPACING)
                .setBackground(Theme.GRAY_800.getARGB())
                .setBorderColor(Theme.GRAY_500.getARGB())
                .build();
    }

    private static @Nullable Modal<PackAliasLayout> buildAliasModal(
            PackedPacksScreen screen,
            PackOptionsContext options,
            PackAssetManager assets
    ) {
        if (!Config.get().isDevMode()) return null;

        PackAliasLayout layout = new PackAliasLayout(screen, options.getConfig(), assets);
        return Modal.builder(screen, layout)
                .addListener(open -> {
                    if (!open) layout.saveAliases();
                })
                .padding(SPACING)
                .build();
    }

    private static Modal<OptionsLayout> buildOptionsModal(PackedPacksScreen screen, IntSupplier maxHeight, PackOptionsContext options) {
        return Modal.builder(screen, new OptionsLayout(Minecraft.getInstance(), maxHeight, options.getUserConfig()))
                .setBackdrop(new ColoredRect(Theme.BLACK.withAlpha(0.5f)))
                .setCaptureFocus(true)
                .padding(SPACING)
                .build();
    }

    List<PackList> packLists() {
        return List.of(this.folderDialog.root(), this.availablePacks.list(), this.currentPacks.list());
    }

    List<ToggleableDialog<?>> dialogs() {
        return this.aliasModal != null
                ? List.of(this.optionsModal, this.contextMenu, this.aliasModal, this.renameModal, this.profilesLayout.getSidebar(), this.folderDialog)
                : List.of(this.optionsModal, this.contextMenu, this.renameModal, this.profilesLayout.getSidebar(), this.folderDialog);
    }
}
