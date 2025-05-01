package io.github.fishstiz.packed_packs.gui.components.layout;

import io.github.fishstiz.fidgetz.gui.components.CyclicButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleButton;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.gui.components.list.AvailablePackList;
import io.github.fishstiz.packed_packs.gui.components.list.Query;
import io.github.fishstiz.packed_packs.gui.event.PackListEventListener;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.pack.PackIconCache;
import org.jetbrains.annotations.NotNull;

public final class AvailablePacksLayout extends PackLayout<AvailablePackList> {
    private ToggleButton<?> compatToggleButton;
    private CyclicButton<Query.SortOption, ?> sortCyclicButton;

    public AvailablePacksLayout(PackIconCache iconCache, PackListEventListener listener, int spacing) {
        super(new AvailablePackList(iconCache, listener), spacing);
    }

    public ToggleButton<?> getCompatToggleButton() {
        return this.compatToggleButton;
    }

    public CyclicButton<Query.SortOption, ?> getSortCyclicButton() {
        return this.sortCyclicButton;
    }

    @Override
    protected void initHeader(@NotNull FlexLayout header) {
        this.compatToggleButton = ToggleButton.builder() // TODO: convert to icons
                .setMessage(ResourceUtil.getText("hide_incompatible"))
                .setDimensions(20, 20)
                .setListener(this.list::hideIncompatible)
                .build();
        this.sortCyclicButton = CyclicButton.builder(this.list::sort, Query.SortOption.values())
                .setPrefix(ResourceUtil.getText("sort"))
                .setDimensions(20, 20)
                .build();

        header.addChild(sortCyclicButton);
        header.addChild(compatToggleButton);
    }
}
