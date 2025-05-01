package io.github.fishstiz.packed_packs.gui.components.layout;

import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.gui.components.list.PackListBase;
import io.github.fishstiz.packed_packs.gui.metadata.GridWrapper;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;

public abstract class PackLayout<T extends PackListBase<?>> {
    protected final T list;
    private final GridWrapper<FlexLayout> header;
    private ToggleableEditBox<?> searchField;
    private FlexLayout layout;

    protected PackLayout(T list, int spacing) {
        this.list = list;
        this.header = new GridWrapper<>(FlexLayout.horizontal(this.list::getWidth).spacing(spacing), spacing);
    }

    protected void initHeader(@NotNull FlexLayout header) {
    }

    public final void init(@NotNull FlexLayout layout) {
        this.layout = layout;

        this.searchField = ToggleableEditBox.builder()
                .setHint(ResourceUtil.getText("search"))
                .setEditable(true)
                .addListener(this.list::search)
                .build();
        this.header.layout().addFlexChild(searchField, false);

        this.initHeader(this.header.layout());
        this.layout.addChild(this.header.layout());
        this.layout.addFlexChild(this.list, true);
        this.layout.arrangeElements();
    }

    public T getList() {
        return this.list;
    }

    public ToggleableEditBox<?> getSearchField() {
        return this.searchField;
    }

    public void setHeaderVisibility(boolean visible) {
        if (layout == null) return;

        this.header.layout().visitWidgets(widget -> widget.visible = visible);

        ScreenRectangle headerRect = this.header.layout().getRectangle();
        int y = visible ? headerRect.bottom() + this.header.spacing() : headerRect.top();
        int height = visible ? layout.getHeight() - headerRect.height() - this.header.spacing() : layout.getHeight();

        this.list.setY(y);
        this.list.setHeight(height);
        this.list.clampScrollAmount();
    }
}
