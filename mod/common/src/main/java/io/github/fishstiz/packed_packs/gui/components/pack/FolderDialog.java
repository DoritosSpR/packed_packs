package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.shapes.GuiRectangle;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.PackMenuHeader;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.fidgetz.util.lang.ObjectsUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;

public class FolderDialog extends ToggleableDialog<PackListContainer> implements ContextMenuContainer {
    private static final Component BACK_TEXT = CommonComponents.GUI_BACK.copy().append(CommonComponents.ELLIPSIS);
    private static final int HEADER_HEIGHT = 16;
    private final FidgetzButton<Void> closeButton;
    private final FidgetzText<Void> folderTitle;
    private final PackListViewModel.Module viewModel;

    public FolderDialog(ScreenContext screenContext, PackListViewModel.Module viewModel) {
        super(builder(screenContext.screen(), new PackListContainer(screenContext, viewModel))
                .setOpen(true)
                .setBackground(DrawUtil.DEMO_BACKGROUND));
        this.viewModel = viewModel;
        this.closeButton = this.addRenderableWidget(FidgetzButton.<Void>builder()
                .makeSquare(CROSS_SPRITE.width)
                .setOnPress(() -> this.setOpen(false))
                .spriteOnly()
                .build());
        this.folderTitle = this.addRenderableWidget(FidgetzText.<Void>builder()
                .setMessage(viewModel.module().getTitle())
                .setHeight(CROSS_SPRITE.height)
                .setOffsetY(1)
                .setShadow(true)
                .build());
        this.root().visible = this.isOpen();
        this.addListener(open -> {
            this.root().visible = open;
            if (!open) viewModel.close();
        });
        this.root().visitWidgets(this::addRenderableWidget);
    }

    private void updateBounds() {
        GuiRectangle bounds = this.getBoundingBox();
        int parentX = bounds.getX();
        int parentY = bounds.getY();
        int parentWidth = bounds.getWidth();
        int parentHeight = bounds.getHeight();

        int left = parentX + SPACING;
        int top = parentY + SPACING;
        int right = (parentX + parentWidth) - SPACING;
        int bottom = (parentY + parentHeight) - SPACING;

        this.root().setPosition(left, top + HEADER_HEIGHT + SPACING);
        this.root().setWidth(right - left);
        this.root().setHeight(bottom - this.root().getY());
        this.closeButton.setPosition(left, top);
        this.folderTitle.setPosition(left + this.closeButton.getWidth() + SPACING, top);
        this.folderTitle.setWidth(bounds.getRight() - this.folderTitle.getX() - SPACING * 2);
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        this.updateBounds();
        super.renderBackground(guiGraphics, x, y, width, height, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderForeground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        int left = this.closeButton.getX();
        int top = this.closeButton.getY();
        this.viewModel.sprite().renderClamped(guiGraphics, left, top, CROSS_SPRITE.width, CROSS_SPRITE.height, partialTick);

        if (this.closeButton.isHovered()) {
            WHITE_OVERLAY.render(guiGraphics, left, top, CROSS_SPRITE.width, CROSS_SPRITE.height);
            CROSS_SPRITE.render(guiGraphics, left, top);
        }
    }

    @Override
    public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
        ContextMenuContainer.super.buildItems(
                builder.when(this.isOpen())
                        .ifTrue(folderMenuBuilder -> folderMenuBuilder
                                .add(new PackMenuHeader(this.viewModel.module(), this.viewModel.sprite()))
                                .simpleItem(BACK_TEXT, () -> this.setOpen(false))
                                .when(this.root().getChildAt(mouseX, mouseY).isEmpty())
                                .ifTrue(b -> b
                                        .whenNonNull(ObjectsUtil.mapOrNull(this.viewModel.module(), FilePack::packed_packs$getPath))
                                        .ifTrue((path, operationsMenuBuilder) -> operationsMenuBuilder
                                                .separator()
                                                .simpleItem(RENAME_FILE_TEXT, this.viewModel::fileModifiable, this.viewModel::openRename)
                                                .simpleItem(DELETE_FILE_TEXT, this.viewModel::fileModifiable, this.viewModel::delete)
                                                .simpleItem(OPEN_FILE_TEXT, () -> PackUtil.openPack(this.viewModel.module()))
                                                .simpleItem(OPEN_PARENT_TEXT, () -> PackUtil.openParent(this.viewModel.module()))
                                        )
                                )
                        ),
                mouseX,
                mouseY
        );
    }

//    public void onRename(Pack pack, Component newName) {
//        if (this.parent != null && pack == this.folderPack) {
//            PackList.Entry entry = this.parent.getEntry(this.folderPack);
//            if (entry != null) {
//                entry.onRename(newName);
//            }
//            this.setOpen(false);
//        }
//    }
}
