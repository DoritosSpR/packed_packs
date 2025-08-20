package io.github.fishstiz.packed_packs.gui.components.pack;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.packed_packs.gui.components.events.*;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.pack.PackAssets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FolderPackList extends CurrentPackList {
    private static final Sprite CLOSE_SPRITE = new Sprite(ResourceUtil.getIcon("cross"), Size.of16());
    private static final int HEADER_HEIGHT = 16;
    private final FidgetzButton<Void> closeButton;
    private PackList parent;
    private FolderPack folderPack;
    private Sprite sprite;

    public FolderPackList(PackAssets packAssets, PackListEventListener listener) {
        super(packAssets, listener);
        this.closeButton = FidgetzButton.<Void>builder()
                .setOnPress(() -> this.sendEvent(new FolderCloseEvent(this)))
                .makeSquare(CLOSE_SPRITE.width)
                .build();
    }

    @Override
    protected @NotNull Entry createEntry(Pack pack, int index) {
        return new SubPackEntry(pack, index);
    }

    @Override
    public boolean isTransferable(Pack pack) {
        return false;
    }

    @Override
    public void renderDroppableZone(GuiGraphics guiGraphics, PackList source, ImmutableList<Pack> payload, Pack trigger, int mouseX, int mouseY, float partialTick) {
        if (source == this) {
            super.renderDroppableZone(guiGraphics, source, payload, trigger, mouseX, mouseY, partialTick);
        }
    }

    private void updateBounds() {
        if (this.parent != null) {
            int parentX = this.parent.getX();
            int parentY = this.parent.getY();
            int parentWidth = this.parent.getWidth();
            int parentHeight = this.parent.getHeight();

            int left = parentX + GuiConstants.SPACING;
            int top = parentY + GuiConstants.SPACING;
            int right = (parentX + parentWidth) - GuiConstants.SPACING;
            int bottom = (parentY + parentHeight) - GuiConstants.SPACING;

            this.closeButton.setPosition(left, top);
            this.setPosition(left, top + HEADER_HEIGHT + GuiConstants.SPACING);
            this.setWidth(right - left);
            this.setHeight(bottom - this.getY());
        }
    }

    private void renderFolderInfo(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.parent != null && this.sprite != null && this.folderPack != null) {
            int parentX = this.parent.getX();
            int left = parentX + GuiConstants.SPACING;
            int top = this.parent.getY() + GuiConstants.SPACING;
            int right = (parentX + this.parent.getWidth()) - GuiConstants.SPACING;

            this.closeButton.setPosition(left, top);
            this.sprite.renderClamped(guiGraphics, left, top, CLOSE_SPRITE.width, CLOSE_SPRITE.height, partialTick);

            this.closeButton.setHovered(this.closeButton.isMouseOver(mouseX, mouseY));
            if (this.closeButton.isHovered()) {
                PackListBase.Entry.OVERLAY.render(guiGraphics, left, top, CLOSE_SPRITE.width, CLOSE_SPRITE.height);
                CLOSE_SPRITE.renderClamped(guiGraphics, left, top, CLOSE_SPRITE.width, CLOSE_SPRITE.height, partialTick);
            }

            var font = Minecraft.getInstance().font;
            int startX = left + CLOSE_SPRITE.width + GuiConstants.SPACING;
            int endY = top + HEADER_HEIGHT;

            DrawUtil.renderScrollingStringLeftAlign(guiGraphics, font, this.folderPack.getTitle(), startX, top, right, endY, Theme.GRAY_800.getARGB(), false);
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.updateBounds();
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        this.renderFolderInfo(guiGraphics, mouseX, mouseY, partialTick);
    }

    public void setParent(PackList parent) {
        this.parent = parent;
        this.updateBounds();
    }

    public @Nullable PackList getParent() {
        return this.parent;
    }

    public void setFolderPack(FolderPack folderPack) {
        this.folderPack = folderPack;

        if (folderPack != null) {
            this.packAssets.getOrLoadIcon(this.folderPack, icon -> this.sprite = new Sprite(icon, Size.of16()));
        }
    }

    public @Nullable FolderPack getFolderPack() {
        return this.folderPack;
    }

    public FidgetzButton<Void> getCloseButton() {
        return this.closeButton;
    }

    protected class SubPackEntry extends Entry {
        protected SubPackEntry(Pack pack, int index) {
            super(pack, index);
        }

        @Override
        public boolean isTransferable() {
            return false;
        }
    }
}
