package io.github.fishstiz.packed_packs.gui.screens;

import io.github.fishstiz.fidgetz.gui.components.Modal;
import io.github.fishstiz.fidgetz.gui.components.ToggleableDialog;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenu;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.components.pack.*;
import io.github.fishstiz.packed_packs.gui.layouts.OptionsLayout;
import io.github.fishstiz.packed_packs.gui.layouts.ProfilesLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.AvailablePacksLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.CurrentPacksLayout;
import io.github.fishstiz.packed_packs.gui.layouts.pack.PackAliasLayout;
import io.github.fishstiz.packed_packs.gui.model.PackedPacksViewModel;
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
        ProfilesLayout profilesLayout,
        AvailablePacksLayout availablePacks,
        CurrentPacksLayout currentPacks,
        FileRenameModal renameModal,
        ContextMenu contextMenu,
        Modal<OptionsLayout> optionsModal,
        @Nullable Modal<PackAliasLayout> aliasModal
) {
    private static boolean warmed = false;

    private static <T> CompletableFuture<T> async(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, Util.backgroundExecutor());
    }

    static Components bootstrap(PackedPacksScreen screen, PackedPacksViewModel viewModel) {
        if (warmed) {
            return new Components(
                    new ProfilesLayout(screen, viewModel.createProfilesSlice()),
                    new AvailablePacksLayout(screen.ctx(), viewModel.createAvailableSlice()),
                    new CurrentPacksLayout(screen.ctx(), viewModel.createEnabledSlice()),
                    new FileRenameModal(screen, viewModel),
                    buildMenu(screen),
                    buildOptionsModal(screen, screen::getMaxHeight, viewModel.getConfig()),
                    buildAliasModal(screen, viewModel)
            );
        }

        var profiles = async(() -> new ProfilesLayout(screen, viewModel.createProfilesSlice()));
        var available = async(() -> new AvailablePacksLayout(screen.ctx(), viewModel.createAvailableSlice()));
        var current = async(() -> new CurrentPacksLayout(screen.ctx(), viewModel.createEnabledSlice()));
        var rename = async(() -> new FileRenameModal(screen, viewModel));
        var menu = async(() -> buildMenu(screen));
        var alias = async(() -> buildAliasModal(screen, viewModel));
        var optionsModal = async(() -> buildOptionsModal(screen, screen::getMaxHeight, viewModel.getConfig()));

        CompletableFuture.allOf(available, current, rename, menu, alias, profiles).join();

        warmed = true;

        return new Components(profiles.join(), available.join(), current.join(), rename.join(), menu.join(), optionsModal.join(), alias.join());
    }

    private static ContextMenu buildMenu(PackedPacksScreen screen) {
        return ContextMenu.builder(screen)
                .setSpacing(SPACING)
                .setBackground(Theme.GRAY_800.getARGB())
                .setBorderColor(Theme.GRAY_500.getARGB())
                .build();
    }

    private static @Nullable Modal<PackAliasLayout> buildAliasModal(PackedPacksScreen screen, PackedPacksViewModel viewModel) {
        if (!Config.get().isDevMode()) return null;

        PackAliasLayout layout = new PackAliasLayout(viewModel);
        return Modal.builder(screen, layout)
                .addListener(open -> {
                    if (!open) layout.onClose();
                })
                .padding(SPACING)
                .build();
    }

    private static Modal<OptionsLayout> buildOptionsModal(PackedPacksScreen screen, IntSupplier maxHeight, Config.Packs config) {
        return Modal.builder(screen, new OptionsLayout(Minecraft.getInstance(), maxHeight, config))
                .setBackdrop(new ColoredRect(Theme.BLACK.withAlpha(0.5f)))
                .setCaptureFocus(true)
                .padding(SPACING)
                .build();
    }

    List<ToggleableDialog<?>> dialogs() {
        return this.aliasModal != null
                ? List.of(this.optionsModal, this.contextMenu, this.aliasModal, this.renameModal, this.profilesLayout.getSidebar())
                : List.of(this.optionsModal, this.contextMenu, this.renameModal, this.profilesLayout.getSidebar());
    }
}
