package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.resourcify.ResourcifyButtons;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ModAdditions {
    private static final FabricLoader LOADER = FabricLoader.getInstance();

    private ModAdditions() {
    }

    public static void appendHeader(PackSelectionScreen packScreen, Component title, FlexLayout header) {
        if (LOADER.isModLoaded("resourcify")) {
            try {
                List<? extends Button> resourcifyButtons = ResourcifyButtons.getButtons(packScreen, title);
                if (resourcifyButtons != null) {
                    for (Button button : resourcifyButtons) {
                        header.addChild(button);
                    }
                }
            } catch (LinkageError | Exception e) {
                PackedPacks.LOGGER.warn("[packed_packs] Could not append buttons from Resourcify.");
            }
        }
    }
}
