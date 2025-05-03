package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.gui.components.CyclicButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleButton;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.gui.components.AvailablePackList;
import io.github.fishstiz.packed_packs.gui.components.Query;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.gui.components.events.QueryEvent;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.pack.PackIconCache;
import org.jetbrains.annotations.NotNull;

public final class AvailablePacksLayout extends PackLayout<AvailablePackList> {
    private final PackListEventListener eventListener;
    private CyclicButton<Query.SortOption, Void> sortButton;
    private ToggleButton<Void> compatButton;

    public AvailablePacksLayout(PackIconCache iconCache, PackListEventListener listener, int spacing) {
        super(new AvailablePackList(iconCache, listener), spacing);

        this.eventListener = listener;
    }

    public CyclicButton<Query.SortOption, Void> getSortButton() {
        return this.sortButton;
    }

    public ToggleButton<Void> getCompatButton() {
        return this.compatButton;
    }

    private void sendQueryEvent() {
        this.eventListener.onEvent(new QueryEvent(this.list));
    }

    @Override
    protected void initHeader(@NotNull FlexLayout header) {
        this.sortButton = CyclicButton.<Query.SortOption, Void>builder(Query.SortOption.values())
                .setPrefix(ResourceUtil.getText("sort"))
                .setWidth(60)
                .setDimensions(20, 20)
                .addListener(this.list::sort)
                .addListener(value -> this.sendQueryEvent())
                .setValue(Query.SortOption.A_Z) // TODO: config
                .build();
        this.compatButton = ToggleButton.<Void>builder() // TODO: convert to icons
                .setMessage(ResourceUtil.getText("hide_incompatible"))
                .setWidth(60)
                .setDimensions(20, 20)
                .addListener(this.list::hideIncompatible)
                .addListener(value -> this.sendQueryEvent())
                .setValue(false) // TODO: config
                .build();

        this.list.sort(this.sortButton.getValue());
        this.list.hideIncompatible(this.compatButton.getValue());

        header.addChild(sortButton);
        header.addChild(compatButton);
    }
}
