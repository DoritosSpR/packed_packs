package io.github.fishstiz.packed_packs.compat.etf;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.compat.FabricMod;
import io.github.fishstiz.packed_packs.compat.ModContext;
import io.github.fishstiz.packed_packs.compat.ModExtensionInternal;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.FabricPreferences;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.server.packs.PackType;

public class ETFModExtension implements ModExtensionInternal {
    @Override
    public ModContext mod() {
        return FabricMod.ETF;
    }

    @Override
    public void onCreateHeader(PackType type, FlexLayout header, PackedPacksScreen screen, PackSelectionScreen original) {
        if (type != PackType.CLIENT_RESOURCES) return;

        this.mod().wrapError(header, screen, (layout, prev) -> {
            if (Config.get().isDevMode() || FabricPreferences.ETF_BUTTON.isEnabled()) {
                layout.addChild(ETFButtonFactory.create(prev));
            }
        });
    }

    @Override
    public void onCreatePreferencesMenu(PackType type, ContextMenuItemBuilder builder) {
        if (type != PackType.CLIENT_RESOURCES) return;

        this.mod().wrapError(builder, b -> b.add(ToggleableHelper.fromPref(FabricPreferences.ETF_BUTTON.get())));
    }
}
