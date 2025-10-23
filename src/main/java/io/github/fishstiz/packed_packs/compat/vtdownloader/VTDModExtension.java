package io.github.fishstiz.packed_packs.compat.vtdownloader;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModExtensionInternal;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

public class VTDModExtension implements ModExtensionInternal {
    @Override
    public Mod mod() {
        return Mod.VTD;
    }

    @Override
    public @Nullable ResourceLocation loadAfter() {
        return Mod.RESPACKOPTS.getInternalId();
    }

    @Override
    public void onCreateHeader(PackType type, FlexLayout header, PackedPacksScreen screen, PackSelectionScreen original) {
        if (type != PackType.CLIENT_RESOURCES) return;

        this.mod().wrapError(header, screen, (layout, prev) -> {
            if (Config.get().isDevMode() || Preferences.INSTANCE.vtdButton.get()) {
                layout.addChild(VTDButtonFactory.create(prev));
            }
        });
    }

    @Override
    public void onCreateEntry(PackType type, PackList.Entry entry) {
        if (type != PackType.CLIENT_RESOURCES) return;

        this.mod().wrapError(entry, e -> {
            if (Config.get().isDevMode() || Preferences.INSTANCE.vtdEditButton.get()) {
                VTDEditButtonWidget widget = VTDEditButtonWidget.create(Minecraft.getInstance().screen, e);
                if (widget != null) e.addTopRenderableOnly(e.prependWidget(widget));
            }
        });
    }

    @Override
    public void onCreatePreferencesMenu(PackType type, ContextMenuItemBuilder builder) {
        if (type != PackType.CLIENT_RESOURCES) return;

        this.mod().wrapError(builder, b -> {
            b.add(ToggleableHelper.fromPref(Preferences.INSTANCE.vtdButton));
            b.add(ToggleableHelper.fromPref(Preferences.INSTANCE.vtdEditButton));
        });
    }
}
