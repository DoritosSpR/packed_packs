package io.github.fishstiz.packed_packs.gui.layouts.pack;

import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.gui.components.pack.CurrentPackList;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.PackFileOperations;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class CurrentPacksLayout extends PackLayout {
    public CurrentPacksLayout(PackOptionsContext options, PackAssetManager assets, PackFileOperations fileOps, PackListEventListener listener) {
        super(new CurrentPackList(options, assets, fileOps, listener));
    }

    @Override
    protected void initHeader(@NotNull FlexLayout header) {
        this.getTransferButton().setMessage(Component.literal("<<"));

        header.addChild(this.getTransferButton());
        header.addFlexChild(this.getSearchField());
    }
}
