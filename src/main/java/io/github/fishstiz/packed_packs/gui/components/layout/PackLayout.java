package io.github.fishstiz.packed_packs.gui.components.layout;

import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import io.github.fishstiz.packed_packs.gui.components.list.PackListBase;
import io.github.fishstiz.packed_packs.gui.metadata.Flex;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.gui.metadata.Flex.applyFlex;

public abstract class PackLayout<T extends PackListBase<?>> {
    protected final LinearLayout header;
    protected final int spacing;
    protected final T list;
    private ToggleableEditBox<Flex> searchField;

    protected PackLayout(T list, int spacing) {
        this.header = LinearLayout.horizontal().spacing(spacing);
        this.spacing = spacing;
        this.list = list;
    }

    protected void initLayout(@NotNull LinearLayout layout) {
    }

    public final void init(@NotNull LinearLayout layout) {
        this.searchField = this.createSearchField(list::search);
        this.searchField.setMetadata(Flex.horizontal(this.list::getWidth, this.spacing));
        this.header.addChild(searchField);

        this.initLayout(layout);

        layout.addChild(this.header);
        layout.addChild(this.list);
    }

    public T getList() {
        return this.list;
    }

    public ToggleableEditBox<Flex> getSearchField() {
        return this.searchField;
    }

    private ToggleableEditBox<Flex> createSearchField(Consumer<String> listener) {
        return ToggleableEditBox.<Flex>builder().setHint(ResourceUtil.getText("search"))
                .setEditable(true)
                .addListener(listener)
                .build();
    }

    public void setHeaderVisibility(boolean visible, int contentHeight) {
        this.header.visitWidgets(widget -> widget.visible = visible);

        ScreenRectangle headerRect = this.header.getRectangle();
        int y = visible ? headerRect.bottom() + this.spacing : headerRect.top();
        int height = visible ? contentHeight - headerRect.height() - this.spacing : contentHeight;

        this.list.setY(y);
        this.list.setHeight(height);
        this.list.clampScrollAmount();
    }

    public void repositionElements() {
        applyFlex(this.header);
    }
}
