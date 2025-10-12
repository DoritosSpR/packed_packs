package io.github.fishstiz.packed_packs.compat.vtdownloader;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.api.ModExtension;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.gui.metadata.Toggleable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

public class VTDModExtension implements ModExtension {
    @Override
    public ResourceLocation id() {
        return Mod.VTD.getInternalId();
    }

    @Override
    public @Nullable ResourceLocation loadAfter() {
        return Mod.RESPACKOPTS.getInternalId();
    }

    @Override
    public void onCreateHeader(PackType type, FlexLayout header, PackSelectionScreen screen) {
        if (type != PackType.CLIENT_RESOURCES) return;

        Mod.VTD.wrapError(header, screen, (layout, prev) -> {
            if (PackedPacks.CONFIG.isDevMode() || Preferences.INSTANCE.vtdButton.get()) {
                layout.addChild(VTDButtonFactory.create(prev));
            }
        });
    }

    @Override
    public void onCreateEntry(PackType type, PackList.Entry entry) {
        if (type != PackType.CLIENT_RESOURCES) return;

        Mod.VTD.wrapError(entry, e -> {
            if (PackedPacks.CONFIG.isDevMode() || Preferences.INSTANCE.vtdEditButton.get()) {
                VTDEditButtonWidget widget = VTDEditButtonWidget.create(Minecraft.getInstance().screen, e);
                if (widget != null) e.addTopRenderableOnly(e.prependWidget(widget));
            }
        });
    }

    @Override
    public void onCreatePreferencesMenu(PackType type, ContextMenuItemBuilder builder) {
        if (type != PackType.CLIENT_RESOURCES) return;

        Mod.VTD.wrapError(builder, b -> {
            b.add(Toggleable.fromPref(Preferences.INSTANCE.vtdButton));
            b.add(Toggleable.fromPref(Preferences.INSTANCE.vtdEditButton));
        });
    }
}
