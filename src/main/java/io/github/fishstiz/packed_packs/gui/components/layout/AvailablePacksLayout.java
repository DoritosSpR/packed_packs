package io.github.fishstiz.packed_packs.gui.components.layout;

import io.github.fishstiz.fidgetz.gui.components.CyclicButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleButton;
import io.github.fishstiz.packed_packs.gui.components.PackListContainer;
import io.github.fishstiz.packed_packs.gui.components.list.AvailablePackList;
import io.github.fishstiz.packed_packs.gui.metadata.Flex;
import io.github.fishstiz.packed_packs.gui.components.list.Query;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.gui.layouts.LinearLayout;
import org.jetbrains.annotations.NotNull;

public final class AvailablePacksLayout extends PackLayout<AvailablePackList> {
    private ToggleButton<?> toggleCompatButton;
    private CyclicButton<Query.SortOption, ?> cycleSortButton;
    private final int buttonSize;

    public AvailablePacksLayout(PackListContainer parent, int spacing, int buttonSize) {
        super(new AvailablePackList(parent), spacing);

        this.buttonSize = buttonSize;
    }

    public ToggleButton<?> getToggleCompatButton() {
        return this.toggleCompatButton;
    }

    public CyclicButton<Query.SortOption, ?> getCycleSortButton() {
        return this.cycleSortButton;
    }

    @Override
    protected void initLayout(@NotNull LinearLayout layout) {
        this.toggleCompatButton = ToggleButton.builder() // TODO: convert to icons
                .setMessage(ResourceUtil.getText("hide_incompatible"))
                .setDimensions(this.buttonSize, this.buttonSize)
                .setListener(this.list::hideIncompatible)
                .build();
        this.cycleSortButton = CyclicButton.builder(this.list::sort, Query.SortOption.values())
                .setPrefix(ResourceUtil.getText("sort"))
                .setDimensions(this.buttonSize, this.buttonSize)
                .build();
        this.getSearchField().setMetadata(Flex.horizontal(this.list::getWidth, this.spacing, toggleCompatButton, cycleSortButton));

        this.header.addChild(cycleSortButton);
        this.header.addChild(toggleCompatButton);
    }
}
