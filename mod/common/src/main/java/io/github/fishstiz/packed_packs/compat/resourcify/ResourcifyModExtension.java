package io.github.fishstiz.packed_packs.compat.resourcify;

import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModContext;
import io.github.fishstiz.packed_packs.compat.ModExtensionInternal;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ResourcifyModExtension implements ModExtensionInternal {
    @Override
    public ModContext mod() {
        return Mod.RESOURCIFY;
    }

    @Override
    public @Nullable ResourceLocation[] loadAfter() {
        return new ResourceLocation[]{Mod.ETF.getInternalId(), ResourceUtil.getResource("vt_downloader")};
    }

    @Override
    public void onCreateHeader(PackType type, FlexLayout header, PackedPacksScreen screen, PackSelectionScreen original) {
        this.mod().wrapError(header, original, (layout, packScreen) -> {
            List<? extends Button> buttons = ResourcifyButtons.getButtons(packScreen);
            if (buttons != null) {
                for (Button button : buttons.reversed()) {
                    layout.addChild(button);
                }
            }
        });
    }
}
