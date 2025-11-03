package io.github.fishstiz.packed_packs.compat.respackopts;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModContext;
import io.github.fishstiz.packed_packs.compat.ModExtensionInternal;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.ModPreferences;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class RespackoptsModExtension implements ModExtensionInternal {
    @Override
    public ModContext mod() {
        return Mod.RESPACKOPTS;
    }

    @Override
    public @Nullable ResourceLocation[] loadAfter() {
        return new ResourceLocation[]{Mod.ETF.getInternalId()};
    }

    @Override
    public void onCreateEntry(PackType type, PackList.Entry entry) {
        if (type != PackType.CLIENT_RESOURCES) return;

        this.mod().wrapError(entry, e -> {
            if (Config.get().isDevMode() || ModPreferences.RESPACKOPTS_BUTTON.isEnabled()) {
                RespackoptsWidget widget = RespackoptsWidget.create(e, e.pack());
                if (widget != null) e.addTopRenderableOnly(e.prependWidget(widget));
            }
        });
    }

    @Override
    public void onCreatePreferencesMenu(PackType type, ContextMenuItemBuilder builder) {
        if (type != PackType.CLIENT_RESOURCES) return;

        this.mod().wrapError(builder, b -> b.add(ToggleableHelper.fromPref(ModPreferences.RESPACKOPTS_BUTTON.get())));
    }

    @Override
    public boolean forceCommitOnClose(PackType type) {
        return type == PackType.CLIENT_RESOURCES && this.mod().wrapError(RespackoptsUtil::isForceReload, false);
    }

    @Override
    public boolean shouldIgnoreChange(PackType type, Path path) {
        return this.mod().wrapError(RespackoptsUtil::isRespackOptsFile, false, path);
    }
}