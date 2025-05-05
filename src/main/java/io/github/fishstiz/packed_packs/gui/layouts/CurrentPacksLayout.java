package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.gui.components.CurrentPackList;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.util.pack.PackIconCache;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class CurrentPacksLayout extends PackLayout<CurrentPackList> {
    public CurrentPacksLayout(PackIconCache iconCache, PackListEventListener listener, int spacing) {
        super(new CurrentPackList(iconCache, listener), spacing);
    }

    @Override
    protected void initHeader(@NotNull FlexLayout header) {
        this.getTransferButton().setMessage(Component.literal("<<"));

        header.addChild(this.getTransferButton());
        header.addFlexChild(this.getSearchField());
    }
}
