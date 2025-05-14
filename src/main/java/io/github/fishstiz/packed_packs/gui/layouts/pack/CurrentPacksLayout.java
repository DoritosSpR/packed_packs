package io.github.fishstiz.packed_packs.gui.layouts.pack;

import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.gui.components.pack.CurrentPackList;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.util.pack.PackAssets;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class CurrentPacksLayout extends PackLayout<CurrentPackList> {
    public CurrentPacksLayout(PackAssets packAssets, PackListEventListener listener, int spacing) {
        super(new CurrentPackList(packAssets, listener), spacing);
    }

    @Override
    protected void initHeader(@NotNull FlexLayout header) {
        this.getTransferButton().setMessage(Component.literal("<<"));

        header.addChild(this.getTransferButton());
        header.addFlexChild(this.getSearchField());
    }
}
