package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.etf.ETFButtonFactory;
import io.github.fishstiz.packed_packs.compat.resourcify.ResourcifyButtons;
import io.github.fishstiz.packed_packs.compat.respackopts.RespackoptsUtil;
import io.github.fishstiz.packed_packs.compat.respackopts.RespackoptsWidget;
import io.github.fishstiz.packed_packs.compat.vtdownloader.VTDButtonFactory;
import io.github.fishstiz.packed_packs.compat.vtdownloader.VTDEditButtonWidget;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

public class ModAdditions {
    private ModAdditions() {
    }

    public static void addToHeader(boolean resourcePacks, FlexLayout header, PackSelectionScreen original) {
        if (resourcePacks) {
            Screen currentScreen = Minecraft.getInstance().screen;

            Mod.ETF.wrapError(header, currentScreen, (layout, previous) -> {
                if (PackedPacks.CONFIG.isDevMode() || Preferences.INSTANCE.etfButton.get()) {
                    layout.addChild(ETFButtonFactory.create(previous));
                }
            });
            Mod.VTD.wrapError(header, currentScreen, (layout, previous) -> {
                if (PackedPacks.CONFIG.isDevMode() || Preferences.INSTANCE.vtdButton.get()) {
                    layout.addChild(VTDButtonFactory.create(previous));
                }
            });
        }

        Mod.RESOURCIFY.wrapError(header, original, original.getTitle(), (layout, packScreen, title) -> {
            List<? extends Button> resourcifyButtons = ResourcifyButtons.getButtons(packScreen, title);
            if (resourcifyButtons != null) {
                for (Button button : resourcifyButtons.reversed()) { // buttons are manually positioned in reverse
                    layout.addChild(button);
                }
            }
        });
    }

    public static void addToEntry(boolean resourcePacks, PackListBase<?>.Entry packListEntry) {
        if (resourcePacks) {
            Mod.RESPACKOPTS.wrapError(packListEntry, entry -> {
                if (PackedPacks.CONFIG.isDevMode() || Preferences.INSTANCE.respackoptsButton.get()) {
                    RespackoptsWidget respackOptsWidget = RespackoptsWidget.create(entry, entry.getPack());
                    if (respackOptsWidget != null) {
                        entry.addTopRenderableOnly(entry.prependWidget(respackOptsWidget));
                    }
                }
            });
            Mod.VTD.wrapError(packListEntry, entry -> {
                if (PackedPacks.CONFIG.isDevMode() || Preferences.INSTANCE.vtdEditButton.get()) {
                    VTDEditButtonWidget vtdEditButtonWidget = VTDEditButtonWidget.create(Minecraft.getInstance().screen, entry);
                    if (vtdEditButtonWidget != null) {
                        entry.addTopRenderableOnly(entry.prependWidget(vtdEditButtonWidget));
                    }
                }
            });
        }
    }

    /**
     * @return mod id requesting reload
     */
    public static @Nullable String shouldCommit(boolean resourcePacks) {
        if (resourcePacks && Mod.RESPACKOPTS.wrapError(RespackoptsWidget::isForceReload, false)) {
            return Mod.RESPACKOPTS.getId();
        }
        return null;
    }

    public static boolean discontinueChanges(Path path) {
        if (Mod.RESPACKOPTS.wrapError(RespackoptsUtil::isRespackOptsFile, false, path)) {
            return true;
        }
        return false;
    }
}
