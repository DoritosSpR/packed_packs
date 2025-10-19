package io.github.fishstiz.packed_packs.compat.respackopts;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModExtensionInternal;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class RespackoptsModExtension implements ModExtensionInternal {
    @Override
    public Mod mod() {
        return Mod.RESPACKOPTS;
    }

    @Override
    public @Nullable ResourceLocation loadAfter() {
        return Mod.ETF.getInternalId();
    }

    @Override
    public void onCreateEntry(PackType type, PackList.Entry entry) {
        if (type != PackType.CLIENT_RESOURCES) return;

        this.mod().wrapError(entry, e -> {
            if (PackedPacks.CONFIG.isDevMode() || Preferences.INSTANCE.respackoptsButton.get()) {
                RespackoptsWidget widget = RespackoptsWidget.create(e, e.pack());
                if (widget != null) e.addTopRenderableOnly(e.prependWidget(widget));
            }
        });
    }

    @Override
    public void onCreatePreferencesMenu(PackType type, ContextMenuItemBuilder builder) {
        if (type != PackType.CLIENT_RESOURCES) return;

        this.mod().wrapError(builder, b -> b.add(ToggleableHelper.fromPref(Preferences.INSTANCE.respackoptsButton)));
    }

    @Override
    public boolean forceCommitOnClose(PackType type) {
        return type == PackType.CLIENT_RESOURCES && this.mod().wrapError(RespackoptsWidget::isForceReload, false);
    }

    @Override
    public boolean shouldIgnoreChange(PackType type, Path path) {
        return this.mod().wrapError(RespackoptsUtil::isRespackOptsFile, false, path);
    }
}