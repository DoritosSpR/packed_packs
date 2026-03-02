package io.github.fishstiz.packed_packs.gui.layouts.pack;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.gui.components.pack.PackListContainer;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import org.jspecify.annotations.NonNull;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.SPACING;

public abstract class PackLayout {
    protected final PackListContainer listContainer;
    private final ToggleableEditBox<Void> searchField;
    private final FidgetzButton<Void> transferButton;
    private FlexLayout headerLayout;
    private FlexLayout layout;

    protected PackLayout(ScreenContext screenContext, PackListViewModel viewModel) {
        this.listContainer = new PackListContainer(screenContext, viewModel);
        this.searchField = ToggleableEditBox.<Void>builder()
                .setHint(ResourceUtil.getText("search").append(CommonComponents.ELLIPSIS))
                .setEditable(true)
                .addListener(viewModel::search)
                .build();
        this.transferButton = FidgetzButton.<Void>builder()
                .makeSquare()
                .setOnPress(viewModel::transferAll)
                .setTooltip(Tooltip.create(ResourceUtil.getText("transfer_all.info")))
                .build();
    }

    protected abstract void initHeader(@NonNull FlexLayout header);

    public final void init(@NonNull FlexLayout layout) {
        this.headerLayout = FlexLayout.horizontal(this.listContainer::getWidth).spacing(SPACING);
        this.initHeader(headerLayout);

        this.layout = layout;
        this.layout.addChild(headerLayout);
        this.layout.addFlexChild(this.listContainer, true);
    }

    public PackListContainer listContainer() {
        return this.listContainer;
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

        this.listContainer.setY(y);
        this.listContainer.setHeight(height);
        this.listContainer.list().clampScrollAmount();
    }
}
