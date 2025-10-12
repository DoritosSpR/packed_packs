package io.github.fishstiz.packed_packs.compat.api;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Register entrypoint under the {@code packed_packs} key
 */
public interface ModExtension {
    /**
     * The identifier
     */
    ResourceLocation id();

    /**
     * @return A {@link ResourceLocation} that this extension should load after.
     */
    default @Nullable ResourceLocation loadAfter() {
        return null;
    }

    /**
     * Invoked when building the header layout for the PackedPacksScreen.
     *
     * @param type   Resource or data packs.
     * @param header The header to add widgets to.
     * @param screen The original vanilla {@link PackSelectionScreen}.
     */
    default void onCreateHeader(PackType type, FlexLayout header, PackSelectionScreen screen) {
    }

    /**
     * Invoked when creating a pack entry.
     *
     * @param type  Resource or data packs.
     * @param entry The entry to modify or add widgets to.
     */
    default void onCreateEntry(PackType type, PackList.Entry entry) {
    }

    /**
     * Invoked when creating the context menu for preferences.
     *
     * @param builder The context menu item builder to add items to.
     */
    default void onCreatePreferencesMenu(PackType type, ContextMenuItemBuilder builder) {
    }

    /**
     * @param type Resource or datapacks.
     * @return {@code true} if a commit should occur when closing the screen.
     */
    default boolean forceCommitOnClose(PackType type) {
        return false;
    }

    /**
     * @param type Resource or datapacks.
     * @param path The changed file path.
     * @return {@code true} to ignore this change.
     */
    default boolean shouldIgnoreChange(PackType type, Path path) {
        return false;
    }
}
