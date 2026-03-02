package io.github.fishstiz.packed_packs.gui.layouts.pack;

import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.gui.components.pack.FolderDialog;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public final class CurrentPacksLayout extends PackLayout {
    public CurrentPacksLayout(ScreenContext screenContext, PackListViewModel packListContext) {
        super(screenContext, packListContext);
    }

    @Override
    protected void initHeader(@NonNull FlexLayout header) {
        this.getTransferButton().setMessage(Component.literal("<<"));

        header.addChild(this.getTransferButton());
        header.addFlexChild(this.getSearchField());
    }
}
