package io.github.fishstiz.packed_packs.gui.layouts.pack;

import io.github.fishstiz.fidgetz.gui.components.CyclicButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleButton;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.events.BasicEvent;
import io.github.fishstiz.packed_packs.gui.components.pack.AvailablePackList;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.PackFileOperations;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class AvailablePacksLayout extends PackLayout {
    private static final Component SORT_TEXT = ResourceUtil.getText("sort");
    private static final Component COMPAT_TEXT = ResourceUtil.getText("hide_incompatible");
    private static final Component COMPAT_INFO = ResourceUtil.getText("hide_incompatible.info");
    private final PackListEventListener eventListener;
    private CyclicButton<Query.SortOption, Void> sortButton;
    private ToggleButton<Void> compatButton;

    public AvailablePacksLayout(PackOptionsContext options, PackAssetManager assets, PackFileOperations fileOps, PackListEventListener listener) {
        super(new AvailablePackList(options, assets, fileOps, listener));
        this.eventListener = listener;
    }

    public CyclicButton<Query.SortOption, Void> getSortButton() {
        return this.sortButton;
    }

    public ToggleButton<Void> getCompatButton() {
        return this.compatButton;
    }

    private void recordEvent() {
        this.eventListener.onEvent(new BasicEvent(this.list));
    }

    @Override
    protected void initHeader(@NotNull FlexLayout header) {
        this.sortButton = CyclicButton.<Query.SortOption, Void>builder(Query.SortOption.values())
                .setPrefix(SORT_TEXT)
                .makeSquare()
                .addListener(value -> this.recordEvent())
                .addListener(this.list::sort)
                .addListener(Config.get()::setSort)
                .setValue(Config.get().getSort())
                .build();
        this.compatButton = ToggleableHelper.applyPref(Preferences.INSTANCE.toggleIncompatibleWidget, ToggleButton.<Void>builder())
                .setMessage(COMPAT_TEXT)
                .setTooltip(Tooltip.create(COMPAT_INFO))
                .setSprite(ToggleButton.Sprites.of(
                        new Sprite(ResourceUtil.getIcon("incompatible_hidden"), Size.of16()),
                        new Sprite(ResourceUtil.getIcon("incompatible"), Size.of16())
                ))
                .makeSquare()
                .addListener(value -> this.recordEvent())
                .addListener(this.list::hideIncompatible)
                .addListener(Config.get()::setHideIncompatible)
                .setValue(Config.get().isHideIncompatible())
                .build();

        this.list.sort(this.sortButton.getValue());
        this.list.hideIncompatible(this.compatButton.getValue());
        this.getTransferButton().setMessage(Component.literal(">>"));

        header.addFlexChild(this.getSearchField());
        header.addChild(sortButton);

        if (Config.get().isDevMode() || Preferences.INSTANCE.toggleIncompatibleWidget.get()) {
            header.addChild(compatButton);
        }
        header.addChild(this.getTransferButton());
    }

    public void saveFilters() {
        Config.get().setHideIncompatible(this.compatButton.getValue());
        Config.get().setSort(this.sortButton.getValue());
    }
}
