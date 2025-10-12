package io.github.fishstiz.packed_packs.compat.etf;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.api.ModExtension;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.metadata.Toggleable;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public class ETFModExtension implements ModExtension {
    @Override
    public ResourceLocation id() {
        return Mod.ETF.getInternalId();
    }

    @Override
    public void onCreateHeader(PackType type, FlexLayout header, PackSelectionScreen screen) {
        if (type != PackType.CLIENT_RESOURCES) return;

        Mod.ETF.wrapError(header, screen, (layout, prev) -> {
            if (PackedPacks.CONFIG.isDevMode() || Preferences.INSTANCE.etfButton.get()) {
                layout.addChild(ETFButtonFactory.create(prev));
            }
        });
    }

    @Override
    public void onCreatePreferencesMenu(PackType type, ContextMenuItemBuilder builder) {
        if (type != PackType.CLIENT_RESOURCES) return;

        Mod.ETF.wrapError(builder, b -> b.add(Toggleable.fromPref(Preferences.INSTANCE.etfButton)));
    }
}
