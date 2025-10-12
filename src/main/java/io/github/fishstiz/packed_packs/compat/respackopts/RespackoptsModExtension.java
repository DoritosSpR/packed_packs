package io.github.fishstiz.packed_packs.compat.respackopts;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.api.ModExtension;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.gui.metadata.Toggleable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class RespackoptsModExtension implements ModExtension {
    @Override
    public ResourceLocation id() {
        return Mod.RESPACKOPTS.getInternalId();
    }

    @Override
    public @Nullable ResourceLocation loadAfter() {
        return Mod.ETF.getInternalId();
    }

    @Override
    public void onCreateEntry(PackType type, PackList.Entry entry) {
        if (type != PackType.CLIENT_RESOURCES) return;

        Mod.RESPACKOPTS.wrapError(entry, e -> {
            if (PackedPacks.CONFIG.isDevMode() || Preferences.INSTANCE.respackoptsButton.get()) {
                RespackoptsWidget widget = RespackoptsWidget.create(e, e.pack());
                if (widget != null) e.addTopRenderableOnly(e.prependWidget(widget));
            }
        });
    }

    @Override
    public void onCreatePreferencesMenu(PackType type, ContextMenuItemBuilder builder) {
        if (type != PackType.CLIENT_RESOURCES) return;

        Mod.RESPACKOPTS.wrapError(builder, b -> b.add(Toggleable.fromPref(Preferences.INSTANCE.respackoptsButton)));
    }

    @Override
    public boolean forceCommitOnClose(PackType type) {
        return type == PackType.CLIENT_RESOURCES && Mod.RESPACKOPTS.wrapError(RespackoptsWidget::isForceReload, false);
    }

    @Override
    public boolean shouldIgnoreChange(PackType type, Path path) {
        return Mod.RESPACKOPTS.wrapError(RespackoptsUtil::isRespackOptsFile, false, path);
    }
}