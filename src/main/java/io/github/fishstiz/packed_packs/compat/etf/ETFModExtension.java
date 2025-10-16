package io.github.fishstiz.packed_packs.compat.etf;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModExtensionInternal;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.metadata.Toggleable;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.server.packs.PackType;

public class ETFModExtension implements ModExtensionInternal {
    @Override
    public Mod mod() {
        return Mod.ETF;
    }

    @Override
    public void onCreateHeader(PackType type, FlexLayout header, PackedPacksScreen screen, PackSelectionScreen original) {
        if (type != PackType.CLIENT_RESOURCES) return;

        this.mod().wrapError(header, screen, (layout, prev) -> {
            if (PackedPacks.CONFIG.isDevMode() || Preferences.INSTANCE.etfButton.get()) {
                layout.addChild(ETFButtonFactory.create(prev));
            }
        });
    }

    @Override
    public void onCreatePreferencesMenu(PackType type, ContextMenuItemBuilder builder) {
        if (type != PackType.CLIENT_RESOURCES) return;

        this.mod().wrapError(builder, b -> b.add(Toggleable.fromPref(Preferences.INSTANCE.etfButton)));
    }
}
