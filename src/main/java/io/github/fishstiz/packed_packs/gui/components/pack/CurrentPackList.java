package io.github.fishstiz.packed_packs.gui.components.pack;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.GradientRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.util.InputUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.gui.components.events.MoveEvent;
import io.github.fishstiz.packed_packs.pack.PackAssets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import static io.github.fishstiz.fidgetz.util.GuiUtil.playClickSound;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.lang.IntsUtil.hasGap;
import static io.github.fishstiz.packed_packs.util.lang.ObjectsUtil.pick;
import static io.github.fishstiz.packed_packs.util.ResourceUtil.getVanillaSprite;

public class CurrentPackList extends PackListBase<CurrentPackList.Entry> {
    private static final Sprite UNSELECT_HIGHLIGHTED_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/unselect_highlighted"));
    private static final Sprite UNSELECT_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/unselect"));
    private static final Sprite MOVE_UP_HIGHLIGHTED_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/move_up_highlighted"));
    private static final Sprite MOVE_UP_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/move_up"));
    private static final Sprite MOVE_DOWN_HIGHLIGHTED_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/move_down_highlighted"));
    private static final Sprite MOVE_DOWN_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/move_down"));
    private static final Theme DROP_THEME = Theme.GREEN_500;
    private static final ColoredRect DROP_INDEX = new ColoredRect(DROP_THEME.getARGB());
    private static final GradientRect SCROLL_UP = GradientRect.fromTop(DROP_THEME.withAlpha(0.75f), DROP_THEME.withAlpha(0));
    private static final GradientRect SCROLL_DOWN = SCROLL_UP.flip();
    private static final int DROP_INDEX_PADDING = 3;
    private static final double SCROLL_STEP = 10;
    private boolean scrolling;

    public CurrentPackList(PackAssets packAssets, PackListEventListener listener) {
        super(packAssets, listener);
    }

    @Override
    protected @NotNull Entry createEntry(Pack pack, int index) {
        return new Entry(pack, index);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        boolean keyPressed = super.keyPressed(keyEvent);
        if (!keyPressed) {
            Entry entry = this.getEntry(this.getLastSelected());
            if (entry != null) {
                if (InputUtil.isMoveDown(keyEvent)) {
                    if (entry.moveDown()) playClickSound();
                    return true;
                } else if (InputUtil.isMoveUp(keyEvent)) {
                    if (entry.moveUp()) playClickSound();
                    return true;
                }
            }
        }
        return keyPressed;
    }

    private void scrollStep(MoveDirection direction, float partialTick) {
        double scrollAmount = this.scrollAmount();
        if (direction.isUp()) {
            scrollAmount -= SCROLL_STEP * partialTick;
        } else if (direction.isDown()) {
            scrollAmount += SCROLL_STEP * partialTick;
        }

        this.scrolling = true;
        this.setClampedScrollAmount(scrollAmount);
    }

    private int getDropIndex(double mouseY) {
        if (this.children().isEmpty()) return -1;

        int index = this.getRowIndex(mouseY);
        if (index == -1) return -1;

        Entry entry = this.getEntry(index);
        int centerY = entry.getY() + (entry.getHeight() / 2);

        if (mouseY >= centerY) {
            int next = index + 1;
            return next < this.children().size() ? next : -1;
        }
        return index;
    }

    private boolean isMouserOverSelection(List<Pack> selection, double mouseX, double mouseY) {
        for (Pack selected : selection) {
            Entry entry = this.getEntry(selected);
            if (entry != null && entry.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    public void insertDrop(Pack pack, int index) {
        if (pack != null) {
            int previous = this.packs.indexOf(pack);
            if (previous != -1 && previous < index) {
                index--;
            }
            this.packs.remove(pack);
            this.packs.add(Math.clamp(index, 0, this.packs.size()), pack);
            this.queryPacks();
        }
    }

    @Override
    public boolean canDrop(PackList source, ImmutableList<Pack> payload, Pack trigger, double mouseX, double mouseY) {
        if (this.scrolling || this.isQueried() || payload.isEmpty() || (source != this && source instanceof FolderPackList)) {
            return false;
        }

        int dropIndex = this.getDropIndex(mouseY);
        if (dropIndex > -1 && this.getEntry(dropIndex).isFixed()) {
            return false;
        }

        if (source != this) {
            return source.isTransferable(trigger);
        }

        if (trigger.isFixedPosition() || this.isMouserOverSelection(payload, mouseX, mouseY)) {
            return false;
        }

        int[] indices = this.getIndicesFromSelection(payload);

        if (indices.length == 0) {
            return false;
        }

        if (hasGap(indices)) {
            return true;
        }

        Arrays.sort(indices);
        int lastSelectionIndex = indices[indices.length - 1];

        if (!this.packs.isEmpty()) {
            int lastPackIndex = this.packs.indexOf(this.packs.getLast());
            if (dropIndex == -1 && lastPackIndex == lastSelectionIndex) {
                return false;
            }
        }

        return dropIndex != indices[0] && dropIndex - 1 != lastSelectionIndex;
    }

    @Override
    protected @Nullable List<Pack> handleDrop(PackList source, ImmutableList<Pack> payload, Pack trigger, double mouseX, double mouseY) {
        if (!this.canDrop(source, payload, trigger, mouseX, mouseY)) return null;

        int dropIndex = this.getDropIndex(mouseY);
        if (dropIndex == -1) {
            dropIndex = this.children().size();
        }

        if (source == this) {
            List<Pack> movable = new ArrayList<>(payload);
            movable.removeIf(Pack::isFixedPosition);
            return this.moveAll(this.orderSelection(movable), dropIndex) ? payload : null;
        }

        this.clearSelection();
        List<Pack> dropped = new ArrayList<>();
        for (Pack selected : payload) {
            if (source.isTransferable(selected)) {
                dropped.add(selected);
                this.insertDrop(selected, dropIndex);
                this.select(selected);
            }
        }
        source.removeAll(dropped);
        this.select(trigger);

        return dropped;
    }

    private void renderDropIndex(GuiGraphics guiGraphics, int mouseY, int x, int width) {
        int dropIndex = this.getDropIndex(mouseY);
        int rowTop = Math.clamp(
                this.getRowTop(dropIndex != -1 ? dropIndex : this.children().size()),
                this.getY() + this.offsetY + DROP_INDEX_PADDING,
                this.getBottom() - this.rowGap - DROP_INDEX_PADDING
        );
        int indexY = rowTop - this.rowGap - DROP_INDEX_PADDING;

        guiGraphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
        DROP_INDEX.render(guiGraphics, x, indexY, width, rowTop - indexY + DROP_INDEX_PADDING);
        guiGraphics.disableScissor();
    }

    @Override
    public void renderDroppableZone(GuiGraphics guiGraphics, PackList source, ImmutableList<Pack> payload, Pack trigger, int mouseX, int mouseY, float partialTick) {
        if (this.isQueried() || (source != this && source instanceof FolderPackList)) return;

        int x = this.getX();
        int y = this.getY();
        int width = this.scrollbarVisible() ? this.getWidth() - this.scrollbarOffset : this.getWidth();
        int height = this.getHeight();
        int bottom = this.getBottom();

        if (this.isMouseOver(mouseX, mouseY)) {
            double scrollAmount = this.scrollAmount();

            int scrollDownY = bottom - this.getItemHeight();
            if (scrollAmount < this.maxScrollAmount() && mouseY >= scrollDownY) {
                SCROLL_DOWN.render(guiGraphics, x, scrollDownY, width, this.getItemHeight());
                this.scrollStep(MoveDirection.DOWN, partialTick);
            } else if (scrollAmount > 0 && mouseY <= y + this.getItemHeight()) {
                SCROLL_UP.render(guiGraphics, x, y, width, this.getItemHeight());
                this.scrollStep(MoveDirection.UP, partialTick);
            } else {
                this.scrolling = false;
            }

            if (this.canDrop(source, payload, trigger, mouseX, mouseY)) {
                this.renderDropIndex(guiGraphics, mouseY, x, width);
            }
        }

        DrawUtil.renderOutline(guiGraphics, x, y, width, height, DROP_THEME.getARGB());
    }

    public boolean isScrolling() {
        return this.scrolling;
    }

    public class Entry extends PackListBase<Entry>.Entry {
        protected Entry(Pack pack, int index) {
            super(pack, index);
        }

        @Override
        public boolean isTransferable() {
            return !this.pack.isRequired() && !this.isStale();
        }

        private int getDownIndex() {
            for (int i = this.index + 1; i < CurrentPackList.this.packs.size(); i++) {
                if (!CurrentPackList.this.packs.get(i).isFixedPosition()) return i;
            }
            return -1;
        }

        private int getUpIndex() {
            for (int i = this.index - 1; i >= 0; i--) {
                if (!CurrentPackList.this.packs.get(i).isFixedPosition()) return i;
            }
            return -1;
        }

        private boolean isFixed() {
            return CurrentPackList.this.isQueried() || this.pack.isFixedPosition() || this.isStale();
        }

        public boolean canMoveDown() {
            if (this.isFixed()) return false;

            int size = CurrentPackList.this.packs.size();

            if (this.isSelected()) {
                List<Pack> selection = CurrentPackList.this.getOrderedSelection().reversed();
                if (!selection.isEmpty()) {
                    Entry entry = CurrentPackList.this.getEntry(selection.getFirst());
                    return entry != null && entry.getIndex() < size && entry.getDownIndex() > -1;
                }
            }

            return this.index >= 0 && this.index < size && this.getDownIndex() > -1;
        }

        public boolean canMoveUp() {
            if (this.isFixed()) return false;

            if (this.isSelected()) {
                List<Pack> selection = CurrentPackList.this.getOrderedSelection();
                if (!selection.isEmpty()) {
                    Entry entry = CurrentPackList.this.getEntry(selection.getFirst());
                    return entry != null && entry.getIndex() > 0 && entry.getUpIndex() > -1;
                }
            }

            return this.index > 0 && this.getUpIndex() > -1;
        }

        public boolean isMouseOverRemove(double mouseX, double mouseY) {
            return CurrentPackList.this.isHovered() && this.isTransferable() && GuiUtil.containsPoint(
                    this.getX() + H_SPACING,
                    this.getY(),
                    UNSELECT_SPRITE.width / 2,
                    UNSELECT_SPRITE.height,
                    mouseX,
                    mouseY
            );
        }

        public boolean isMouseOverUp(double mouseX, double mouseY) {
            return CurrentPackList.this.isHovered() && this.canMoveUp() && GuiUtil.containsPoint(
                    this.getX() + H_SPACING + MOVE_UP_SPRITE.width / 2,
                    this.getY(),
                    MOVE_UP_SPRITE.width / 2,
                    MOVE_UP_SPRITE.height / 2,
                    mouseX,
                    mouseY
            );
        }

        public boolean isMouseOverDown(double mouseX, double mouseY) {
            return CurrentPackList.this.isHovered() && this.canMoveDown() && GuiUtil.containsPoint(
                    this.getX() + H_SPACING + MOVE_DOWN_SPRITE.width / 2,
                    this.getY() + MOVE_DOWN_SPRITE.height / 2,
                    MOVE_DOWN_SPRITE.width / 2,
                    MOVE_DOWN_SPRITE.height / 2,
                    mouseX,
                    mouseY
            );
        }

        private @Nullable List<Pack> moveSelection(List<Pack> selection, ToIntFunction<Entry> indexGetter) {
            List<Pack> moved = new ArrayList<>();

            Pack lastSelected = CurrentPackList.this.getLastSelected();
            for (int i = 0; i < selection.size(); i++) {
                Entry entry = CurrentPackList.this.getEntry(selection.get(i));
                if (entry != null && CurrentPackList.this.move(selection.get(i), indexGetter.applyAsInt(entry))) {
                    moved.add(selection.get(i));
                } else if (i == 0) {
                    return null;
                }
            }
            CurrentPackList.this.select(lastSelected);

            return !moved.isEmpty() ? moved : null;
        }

        private void sendMoveEvent(List<Pack> moved) {
            CurrentPackList.this.sendEvent(new MoveEvent(CurrentPackList.this, moved, this.pack));
        }

        private boolean moveDirection(MoveDirection direction) {
            if ((direction.isUp() && !this.canMoveUp()) || (direction.isDown() && !this.canMoveDown())) {
                return false;
            }

            if (!this.isSelected()) {
                int targetIndex = direction.isUp() ? this.getUpIndex() : this.getDownIndex();
                if (CurrentPackList.this.move(this.pack, targetIndex)) {
                    CurrentPackList.this.selectExclusive(this.pack);
                    this.sendMoveEvent(List.of(this.pack));
                    return true;
                }
                return false;
            }

            List<Pack> moved = direction.isUp()
                    ? moveSelection(CurrentPackList.this.getOrderedSelection(), Entry::getUpIndex)
                    : moveSelection(CurrentPackList.this.getOrderedSelection().reversed(), Entry::getDownIndex);

            if (moved != null && !moved.isEmpty()) {
                this.sendMoveEvent(moved);
                return true;
            }

            return false;
        }

        public boolean moveUp() {
            return this.moveDirection(MoveDirection.UP);
        }

        public boolean moveDown() {
            return this.moveDirection(MoveDirection.DOWN);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
            if (isLeftClick(mouseButtonEvent)) {
                double mouseX = mouseButtonEvent.x();
                double mouseY = mouseButtonEvent.y();

                if (this.isMouseOverRemove(mouseX, mouseY)) {
                    return this.consumeClick(CurrentPackList.Entry::transfer);
                } else if (this.isMouseOverUp(mouseX, mouseY)) {
                    return this.consumeClick(CurrentPackList.Entry::moveUp);
                } else if (this.isMouseOverDown(mouseX, mouseY)) {
                    return this.consumeClick(CurrentPackList.Entry::moveDown);
                }
            }
            return super.mouseClicked(mouseButtonEvent, doubleClicked);
        }

        private boolean consumeClick(Consumer<CurrentPackList.Entry> action) {
            playClickSound();
            action.accept(this);
            return false;
        }

        @Override
        protected void renderForeground(GuiGraphics guiGraphics, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            if (!hovering && !this.isSelectedLast()) return;

            int x = left + H_SPACING;
            GuiConstants.WHITE_OVERLAY.render(guiGraphics, x, top, UNSELECT_SPRITE.width, UNSELECT_SPRITE.height);

            if (this.isTransferable()) {
                boolean overRemove = this.isMouseOverRemove(mouseX, mouseY);
                pick(!overRemove, UNSELECT_SPRITE, UNSELECT_HIGHLIGHTED_SPRITE).render(guiGraphics, x, top);
                this.updateCursor(guiGraphics, overRemove);
            }
            if (this.canMoveUp()) {
                boolean overUp = this.isMouseOverUp(mouseX, mouseY);
                pick(!overUp, MOVE_UP_SPRITE, MOVE_UP_HIGHLIGHTED_SPRITE).render(guiGraphics, x, top);
                this.updateCursor(guiGraphics, overUp);
            }
            if (this.canMoveDown()) {
                boolean overDown = this.isMouseOverDown(mouseX, mouseY);
                pick(!overDown, MOVE_DOWN_SPRITE, MOVE_DOWN_HIGHLIGHTED_SPRITE).render(guiGraphics, x, top);
                this.updateCursor(guiGraphics, overDown);
            }
        }

        private void updateCursor(GuiGraphics guiGraphics, boolean hoveringButton) {
            if (hoveringButton) {
                guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        }
    }

    private enum MoveDirection {
        UP, DOWN;

        public boolean isUp() {
            return this == MoveDirection.UP;
        }

        public boolean isDown() {
            return this == MoveDirection.DOWN;
        }
    }
}
