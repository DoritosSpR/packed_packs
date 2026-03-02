package io.github.fishstiz.packed_packs.gui.layouts.pack;

import io.github.fishstiz.fidgetz.gui.components.CyclicButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleButton;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.pack.FolderDialog;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public final class AvailablePacksLayout extends PackLayout {
    private static final Component SORT_TEXT = ResourceUtil.getText("sort");
    private static final Component COMPAT_TEXT = ResourceUtil.getText("hide_incompatible");
    private static final Component COMPAT_INFO = ResourceUtil.getText("hide_incompatible.info");
    private final PackListViewModel viewModel;
    private CyclicButton<Query.SortOption, Void> sortButton;
    private ToggleButton<Void> compatButton;

    public AvailablePacksLayout(ScreenContext screenContext, PackListViewModel viewModel) {
        super(screenContext, viewModel);
        this.viewModel = viewModel;
    }

    public CyclicButton<Query.SortOption, Void> getSortButton() {
        return this.sortButton;
    }

    public ToggleButton<Void> getCompatButton() {
        return this.compatButton;
    }

    @Override
    protected void initHeader(@NonNull FlexLayout header) {
        this.sortButton = CyclicButton.<Query.SortOption, Void>builder(Query.SortOption.values())
                .setPrefix(SORT_TEXT)
                .makeSquare()
                .addListener(this.viewModel::sort)
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
                .addListener(this.viewModel::hideIncompatible)
                .addListener(Config.get()::setHideIncompatible)
                .setValue(Config.get().isHideIncompatible())
                .build();

        this.getTransferButton().setMessage(Component.literal(">>"));
        header.addFlexChild(this.getSearchField());
        header.addChild(sortButton);
        if (Config.get().isDevMode() || Preferences.INSTANCE.toggleIncompatibleWidget.get()) {
            header.addChild(compatButton);
        }
        header.addChild(this.getTransferButton());
    }
}
