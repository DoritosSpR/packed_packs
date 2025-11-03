package io.github.fishstiz.packed_packs.gui.layouts.pack;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.NotNull;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.SPACING;

public abstract class PackLayout {
    protected final PackList list;
    private final ToggleableEditBox<Void> searchField;
    private final FidgetzButton<Void> transferButton;
    private FlexLayout headerLayout;
    private FlexLayout layout;

    protected PackLayout(PackList list) {
        this.list = list;
        this.searchField = ToggleableEditBox.<Void>builder()
                .setHint(ResourceUtil.getText("search").append(CommonComponents.ELLIPSIS))
                .setEditable(true)
                .addListener(this.list::search)
                .build();
        this.transferButton = FidgetzButton.<Void>builder()
                .makeSquare()
                .setOnPress(this.list::transferAll)
                .setTooltip(Tooltip.create(ResourceUtil.getText("transfer_all.info")))
                .build();
    }

    protected abstract void initHeader(@NotNull FlexLayout header);

    public final void init(@NotNull FlexLayout layout) {
        this.headerLayout = FlexLayout.horizontal(this.list::getWidth).spacing(SPACING);
        this.initHeader(headerLayout);

        this.layout = layout;
        this.layout.addChild(headerLayout);
        this.layout.addFlexChild(this.list, true);
    }

    public PackList list() {
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

        this.headerLayout.visitWidgets(widget -> widget.visible = visible);

        ScreenRectangle headerRect = this.headerLayout.getRectangle();
        int y = visible ? headerRect.bottom() + SPACING : headerRect.top();
        int height = visible ? layout.getHeight() - headerRect.height() - SPACING : layout.getHeight();

        this.list.setY(y);
        this.list.setHeight(height);
        this.list.clampScrollAmount();
    }
}
