package io.github.fishstiz.packed_packs.gui.layouts.pack;

import io.github.fishstiz.fidgetz.gui.components.CyclicButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleButton;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.gui.components.pack.AvailablePackList;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.gui.components.events.QueryEvent;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.pack.PackIconCache;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class AvailablePacksLayout extends PackLayout<AvailablePackList> {
    private static final Component SORT_TEXT = ResourceUtil.getText("sort");
    private static final Tooltip SORT_INFO_A_Z = Tooltip.create(ResourceUtil.getText("sort.a_z"));
    private static final Tooltip SORT_INFO_Z_A = Tooltip.create(ResourceUtil.getText("sort.z_a"));
    private static final Tooltip SORT_INFO_RECENT = Tooltip.create(ResourceUtil.getText("sort.recent"));
    private static final Tooltip SORT_INFO_OLDEST = Tooltip.create(ResourceUtil.getText("sort.oldest"));
    private static final Component COMPAT_TEXT = ResourceUtil.getText("hide_incompatible");
    private static final Component COMPAT_INFO = ResourceUtil.getText("hide_incompatible.info");
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
                .setPrefix(SORT_TEXT)
                .setWidth(60)
                .setDimensions(20, 20)
                .addListener(this.list::sort)
                .addListener(value -> this.sendQueryEvent())
                .addListener(this::onCycleSort)
                .setValue(PackedPacks.CONFIG.getSort())
                .setTooltip(this.getSortTooltip(PackedPacks.CONFIG.getSort()))
                .build();
        this.compatButton = ToggleButton.<Void>builder() // TODO: convert to icons
                .setMessage(COMPAT_TEXT)
                .setTooltip(Tooltip.create(COMPAT_INFO))
                .setWidth(60)
                .setDimensions(20, 20)
                .addListener(this.list::hideIncompatible)
                .addListener(value -> this.sendQueryEvent())
                .addListener(PackedPacks.CONFIG::setHideIncompatible)
                .setValue(PackedPacks.CONFIG.isHideIncompatible())
                .build();

        this.list.sort(this.sortButton.getValue());
        this.list.hideIncompatible(this.compatButton.getValue());
        this.getTransferButton().setMessage(Component.literal(">>"));

        header.addFlexChild(this.getSearchField());
        header.addChild(sortButton);
        header.addChild(compatButton);
        header.addChild(this.getTransferButton());
    }

    private void onCycleSort(Query.SortOption sort) {
        this.sortButton.setTooltip(this.getSortTooltip(sort));
        PackedPacks.CONFIG.setSort(sort);
    }

    private Tooltip getSortTooltip(Query.SortOption sort) {
        return switch (sort) {
            case A_Z -> SORT_INFO_A_Z;
            case Z_A -> SORT_INFO_Z_A;
            case OLDEST -> SORT_INFO_OLDEST;
            case RECENT -> SORT_INFO_RECENT;
        };
    }
}
