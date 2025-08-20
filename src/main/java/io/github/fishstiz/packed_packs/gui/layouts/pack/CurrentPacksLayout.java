package io.github.fishstiz.packed_packs.gui.layouts.pack;

import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.gui.components.pack.CurrentPackList;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.pack.PackAssets;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class CurrentPacksLayout extends PackLayout<CurrentPackList> {
    public CurrentPacksLayout(PackAssets packAssets, PackListEventListener listener) {
        super(new CurrentPackList(packAssets, listener));
    }

    @Override
    protected void initHeader(@NotNull FlexLayout header) {
        this.getTransferButton().setMessage(Component.literal("<<"));

        header.addChild(this.getTransferButton());
        header.addFlexChild(this.getSearchField());
    }
}
