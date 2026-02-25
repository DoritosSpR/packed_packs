package io.github.fishstiz.packed_packs.gui.layouts.pack;

import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.gui.components.pack.CurrentPackList;
import io.github.fishstiz.packed_packs.gui.components.pack.PackListProps;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public final class CurrentPacksLayout extends PackLayout {
    public CurrentPacksLayout(PackListProps props) {
        super(CurrentPackList::new, props);
    }

    @Override
    protected void initHeader(@NonNull FlexLayout header) {
        this.getTransferButton().setMessage(Component.literal("<<"));

        header.addChild(this.getTransferButton());
        header.addFlexChild(this.getSearchField());
    }
}
