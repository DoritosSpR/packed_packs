package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.etf.ETFButtonFactory;
import io.github.fishstiz.packed_packs.compat.resourcify.ResourcifyButtons;
import io.github.fishstiz.packed_packs.compat.respackopts.RespackoptsUtil;
import io.github.fishstiz.packed_packs.compat.respackopts.RespackoptsWidget;
import io.github.fishstiz.packed_packs.compat.vtdownloader.VTDButtonFactory;
import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ModAdditions {
    private ModAdditions() {
    }

    public static void addToHeader(boolean resourcePacks, FlexLayout header, PackSelectionScreen original) {
        if (resourcePacks) {
            Screen currentScreen = Minecraft.getInstance().screen;

            Mod.ETF.wrapError(header, currentScreen, (layout, previous) -> layout.addChild(ETFButtonFactory.create(previous)));
            Mod.VTD.wrapError(header, currentScreen, (layout, previous) -> layout.addChild(VTDButtonFactory.create(previous)));
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
                RespackoptsWidget respackOptsWidget = RespackoptsWidget.create(entry, entry.getPack());
                if (respackOptsWidget != null) {
                    entry.prependWidget(respackOptsWidget);
                    entry.addRenderableOnly(respackOptsWidget);
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

    public static boolean discontinueChanges(Path watched, Path path) {
        if (Mod.RESPACKOPTS.wrapError(RespackoptsUtil::isRespackOptsFile, false, path)) {
            return true;
        }
        return false;
    }

    public enum Mod {
        RESOURCIFY("resourcify"),
        RESPACKOPTS("respackopts"),
        ETF("entity_texture_features"),
        VTD("vt_downloader");

        private final boolean loaded;
        private final String id;

        Mod(String id) {
            this.id = id;
            this.loaded = FabricLoader.getInstance().isModLoaded(id);
        }

        public String getId() {
            return this.id;
        }

        public boolean isLoaded() {
            return this.loaded;
        }

        private void logError(Throwable e) {
            PackedPacks.LOGGER.warn("[packed_packs] Error occurred while applying compatibility for mod '{}'", this.getId(), e);
        }

        public <T> T wrapError(Supplier<T> supplier, T defaultValue) {
            try {
                if (this.isLoaded()) {
                    return supplier.get();
                }
            } catch (LinkageError | Exception e) {
                this.logError(e);
            }
            return defaultValue;
        }

        public <T, R> R wrapError(Function<T, R> mapper, R defaultValue, T value) {
            try {
                if (this.isLoaded()) {
                    return mapper.apply(value);
                }
            } catch (LinkageError | Exception e) {
                this.logError(e);
            }
            return defaultValue;
        }

        public <T> void wrapError(T arg, Consumer<T> consumer) {
            try {
                if (this.isLoaded()) {
                    consumer.accept(arg);
                }
            } catch (LinkageError | Exception e) {
                this.logError(e);
            }
        }

        public <T1, T2> void wrapError(T1 arg1, T2 arg2, BiConsumer<T1, T2> consumer) {
            try {
                if (this.isLoaded()) {
                    consumer.accept(arg1, arg2);
                }
            } catch (LinkageError | Exception e) {
                this.logError(e);
            }
        }

        public <T1, T2, T3> void wrapError(T1 arg1, T2 arg2, T3 arg3, TriConsumer<T1, T2, T3> consumer) {
            try {
                if (this.isLoaded()) {
                    consumer.accept(arg1, arg2, arg3);
                }
            } catch (LinkageError | Exception e) {
                this.logError(e);
            }
        }
    }
}
