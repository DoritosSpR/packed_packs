package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.GuiRectangle;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.PackMenuHeader;
import io.github.fishstiz.packed_packs.gui.components.events.*;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.PackFileOperations;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.fidgetz.util.lang.ObjectsUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;

public class FolderDialog extends ToggleableDialog<FolderPackList> implements ContextMenuContainer {
    private static final Component BACK_TEXT = CommonComponents.GUI_BACK.copy().append(CommonComponents.ELLIPSIS);
    private static final int HEADER_HEIGHT = 16;
    private final FidgetzButton<Void> closeButton;
    private final FidgetzText<Void> folderTitle;
    private final PackFileOperations fileOps;
    private final PackListEventListener listener;
    private Sprite folderSprite = PackAssetManager.DEFAULT_FOLDER_ICON;
    private PackList parent;
    private FolderPack folderPack;

    public <S extends Screen & ToggleableDialogContainer & PackListEventListener> FolderDialog(
            S screen,
            PackOptionsContext options,
            PackAssetManager assets,
            PackFileOperations fileOps
    ) {
        super(builder(screen, new FolderPackList(options, assets, fileOps, screen)).setBackground(DrawUtil.DEMO_BACKGROUND));

        this.listener = screen;
        this.fileOps = fileOps;
        this.closeButton = this.addRenderableWidget(
                FidgetzButton.<Void>builder()
                        .setOnPress(() -> this.sendEvent(new FolderCloseEvent(this.root(), this.folderPack)))
                        .makeSquare(CROSS_SPRITE.width)
                        .spriteOnly()
                        .build()
        );
        this.folderTitle = this.addRenderableWidget(
                FidgetzText.<Void>builder()
                        .setHeight(CROSS_SPRITE.height)
                        .setOffsetY(1)
                        .setShadow(true)
                        .alignLeft()
                        .build()
        );

        this.root().visible = false;
        this.addListener(open -> {
            this.root().visible = open;
            if (!open) this.sendEvent(new FolderCloseEvent(this.root(), this.folderPack));
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

    public void updateFolder(PackList parent, FolderPack folderPack, PackAssetManager assets) {
        this.parent = parent;
        this.folderPack = folderPack;
        this.folderTitle.setMessage(folderPack.getTitle());
        assets.getOrLoadIcon(folderPack, icon -> this.folderSprite = icon);

        this.setBoundingBox(parent);
        this.updateBounds();
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
        this.folderSprite.renderClamped(guiGraphics, left, top, CROSS_SPRITE.width, CROSS_SPRITE.height, partialTick);

        if (this.closeButton.isHovered()) {
            WHITE_OVERLAY.render(guiGraphics, left, top, CROSS_SPRITE.width, CROSS_SPRITE.height);
            CROSS_SPRITE.render(guiGraphics, left, top);
        }
    }

    public @Nullable PackList getParent() {
        return this.parent;
    }

    public @Nullable FolderPack getFolderPack() {
        return this.folderPack;
    }

    @Override
    public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
        ContextMenuContainer.super.buildItems(
                builder.when(this.folderPack != null && this.isOpen())
                        .ifTrue(folderMenuBuilder -> folderMenuBuilder
                                .add(new PackMenuHeader(this.folderPack, this.folderSprite))
                                .simpleItem(BACK_TEXT, () -> this.setOpen(false))
                                .when(this.root().getChildAt(mouseX, mouseY).isEmpty())
                                .ifTrue(b -> b
                                        .whenNonNull(ObjectsUtil.mapOrNull(this.folderPack, FilePack::packed_packs$getPath))
                                        .ifTrue((path, operationsMenuBuilder) -> operationsMenuBuilder
                                                .separator()
                                                .simpleItem(RENAME_FILE_TEXT, this::canOperateFolder, this::renameDirectory)
                                                .simpleItem(DELETE_FILE_TEXT, this::canOperateFolder, this::deleteDirectory)
                                                .simpleItem(OPEN_FILE_TEXT, () -> PackUtil.openPack(this.folderPack))
                                                .simpleItem(OPEN_PARENT_TEXT, () -> PackUtil.openParent(this.folderPack))
                                        )
                                )
                        ),
                mouseX,
                mouseY
        );
    }

    private boolean canOperateFolder() {
        return ObjectsUtil.testNullable(this.folderPack, this.fileOps::isOperable) &&
               PackUtil.validatePackPath(this.folderPack) != null;
    }

    private void renameDirectory() {
        if (this.folderPack != null) {
            this.sendEvent(new FileRenameOpenEvent(this.root(), this.folderPack));
        }
    }

    private void deleteDirectory() {
        if (this.fileOps.deletePack(this.folderPack)) {
            this.setOpen(false);
            this.root().remove(this.folderPack);
            this.sendEvent(new FileDeleteEvent(this.root()));
        }
    }

    public void onRename(Pack pack, Component newName) {
        if (this.parent != null && pack == this.folderPack) {
            PackList.Entry entry = this.parent.getEntry(this.folderPack);
            if (entry != null) {
                entry.onRename(newName);
            }
            this.setOpen(false);
        }
    }

    private void sendEvent(PackListEvent event) {
        this.listener.onEvent(event);
    }
}
