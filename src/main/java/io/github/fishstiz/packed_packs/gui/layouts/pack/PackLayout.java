package io.github.fishstiz.packed_packs.gui.layouts.pack;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;
import io.github.fishstiz.packed_packs.gui.metadata.GridWrapper;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public abstract class PackLayout {
    private static final Component SEARCH_HINT = ResourceUtil.getText("search");
    private static final Component TRANSFER_INFO = ResourceUtil.getText("transfer_all.info");
    protected final PackListBase list;
    private final GridWrapper<FlexLayout> header;
    private final ToggleableEditBox<Void> searchField;
    private final FidgetzButton<Void> transferButton;
    private FlexLayout layout;

    protected PackLayout(PackListBase list) {
        this.list = list;
        this.header = new GridWrapper<>(FlexLayout.horizontal(this.list::getWidth).spacing(GuiConstants.SPACING), GuiConstants.SPACING);
        this.searchField = ToggleableEditBox.<Void>builder()
                .setHint(SEARCH_HINT)
                .setEditable(true)
                .addListener(this.list::search)
                .build();
        this.transferButton = FidgetzButton.<Void>builder()
                .makeSquare()
                .setOnPress(this.list::transferAll)
                .setTooltip(Tooltip.create(TRANSFER_INFO))
                .build();
    }

    protected void initHeader(@NotNull FlexLayout header) {
    }

    public final void init(@NotNull FlexLayout layout) {
        this.layout = layout;

        this.initHeader(this.header.layout());
        this.layout.addChild(this.header.layout());
        this.layout.addFlexChild(this.list, true);
        this.layout.arrangeElements();
    }

    public PackListBase getList() {
        return this.list;
    }

    public ToggleableEditBox<Void> getSearchField() {
        return this.searchField;
    }

    public FidgetzButton<Void> getTransferButton() {
        return this.transferButton;
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
