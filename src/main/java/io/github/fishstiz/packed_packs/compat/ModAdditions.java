package io.github.fishstiz.packed_packs.compat;

import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.resourcify.ResourcifyButtons;
import io.github.fishstiz.packed_packs.compat.respackopts.RespackoptsWidget;
import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ModAdditions {
    private ModAdditions() {
    }

    public static void addToHeader(PackSelectionScreen packScreen, Component title, FlexLayout header) {
        Mod.RESOURCIFY.wrapError(packScreen, title, header, (screen, component, layout) -> {
            List<? extends Button> resourcifyButtons = ResourcifyButtons.getButtons(screen, component);
            if (resourcifyButtons != null) {
                for (Button button : resourcifyButtons) {
                    layout.addChild(button);
                }
            }
        });
    }

    public static void addToEntry(PackListBase<?>.Entry packListEntry, Pack pack) {
        Mod.RESPACKOPTS.wrapError(packListEntry, pack, (entry, p) -> {
            RespackoptsWidget respackOptsWidget = RespackoptsWidget.create(entry, p);
            if (respackOptsWidget != null) {
                entry.prependWidget(respackOptsWidget);
                entry.addRenderableOnly(respackOptsWidget);
            }
        });
    }

    /**
     * @return mod id requesting reload
     */
    public static @Nullable String shouldCommit() {
        if (Mod.RESPACKOPTS.wrapError(RespackoptsWidget::isForceReload, false)) {
            return Mod.RESPACKOPTS.getId();
        }
        return null;
    }

    public enum Mod {
        RESOURCIFY("resourcify"),
        RESPACKOPTS("respackopts");

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

        private void logError() {
            PackedPacks.LOGGER.warn("[packed_packs] Error occurred while applying compatibility for mod '{}'", this.getId());
        }

        public <T> T wrapError(Supplier<T> supplier, T defaultValue) {
            try {
                if (this.isLoaded()) {
                    return supplier.get();
                }
            } catch (LinkageError | Exception e) {
                this.logError();
            }
            return defaultValue;
        }

        public <T1, T2> void wrapError(T1 arg1, T2 arg2, BiConsumer<T1, T2> consumer) {
            try {
                if (this.isLoaded()) {
                    consumer.accept(arg1, arg2);
                }
            } catch (LinkageError | Exception e) {
                this.logError();
            }
        }

        public <T1, T2, T3> void wrapError(T1 arg1, T2 arg2, T3 arg3, TriConsumer<T1, T2, T3> consumer) {
            try {
                if (this.isLoaded()) {
                    consumer.accept(arg1, arg2, arg3);
                }
            } catch (LinkageError | Exception e) {
                this.logError();
            }
        }
    }
}
