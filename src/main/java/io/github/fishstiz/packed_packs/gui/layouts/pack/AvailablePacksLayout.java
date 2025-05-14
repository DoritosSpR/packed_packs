package io.github.fishstiz.packed_packs.gui.layouts.pack;

import io.github.fishstiz.fidgetz.gui.components.CyclicButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleButton;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
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

import java.nio.file.Path;

public final class AvailablePacksLayout extends PackLayout<AvailablePackList> {
    private static final Component SORT_TEXT = ResourceUtil.getText("sort");
    private static final Component COMPAT_TEXT = ResourceUtil.getText("hide_incompatible");
    private static final Component COMPAT_INFO = ResourceUtil.getText("hide_incompatible.info");
    private final PackListEventListener eventListener;
    private CyclicButton<Query.SortOption, Void> sortButton;
    private ToggleButton<Void> compatButton;

    public AvailablePacksLayout(Path packDir, PackIconCache iconCache, PackListEventListener listener, int spacing) {
        super(new AvailablePackList(packDir, iconCache, listener), spacing);

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
                .makeSquare()
                .addListener(value -> this.sendQueryEvent())
                .addListener(this.list::sort)
                .addListener(PackedPacks.CONFIG::setSort)
                .setValue(PackedPacks.CONFIG.getSort())
                .build();
        this.compatButton = ToggleButton.<Void>builder()
                .setMessage(COMPAT_TEXT)
                .setTooltip(Tooltip.create(COMPAT_INFO))
                .setSpriteOnly(ToggleButton.Sprites.of(
                        new Sprite(ResourceUtil.getIcon("incompatible_hidden"), Size.of16()),
                        new Sprite(ResourceUtil.getIcon("incompatible"), Size.of16())
                ))
                .makeSquare()
                .addListener(value -> this.sendQueryEvent())
                .addListener(this.list::hideIncompatible)
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
}
