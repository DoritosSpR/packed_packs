package io.github.fishstiz.packed_packs.compat.resourcify;

import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModExtensionInternal;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ResourcifyModExtension implements ModExtensionInternal {
    @Override
    public Mod mod() {
        return Mod.RESOURCIFY;
    }

    @Override
    public @Nullable ResourceLocation loadAfter() {
        return Mod.VTD.getInternalId();
    }

    @Override
    public void onCreateHeader(PackType type, FlexLayout header, PackedPacksScreen screen, PackSelectionScreen original) {
        this.mod().wrapError(header, original, screen.getTitle(), (layout, packScreen, title) -> {
            List<? extends Button> buttons = ResourcifyButtons.getButtons(packScreen, title);
            if (buttons != null) {
                for (Button button : buttons.reversed()) {
                    layout.addChild(button);
                }
            }
        });
    }
}
