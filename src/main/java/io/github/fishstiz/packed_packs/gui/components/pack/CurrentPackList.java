package io.github.fishstiz.packed_packs.gui.components.pack;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.*;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.GradientRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.util.InputUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.gui.components.events.MoveEvent;
import io.github.fishstiz.packed_packs.pack.PackAssets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.*;

import static io.github.fishstiz.fidgetz.util.GuiUtil.playClickSound;
import static io.github.fishstiz.packed_packs.gui.metadata.Toggleable.getDefaultIcon;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;
import static io.github.fishstiz.packed_packs.util.lang.IntsUtil.hasGap;
import static io.github.fishstiz.packed_packs.util.lang.ObjectsUtil.*;
import static io.github.fishstiz.packed_packs.util.ResourceUtil.getVanillaSprite;

public class CurrentPackList extends PackListBase<CurrentPackList.Entry> {
    private static final Sprite UNSELECT_HIGHLIGHTED_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/unselect_highlighted"));
    private static final Sprite UNSELECT_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/unselect"));
    private static final Sprite MOVE_UP_HIGHLIGHTED_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/move_up_highlighted"));
    private static final Sprite MOVE_UP_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/move_up"));
    private static final Sprite MOVE_DOWN_HIGHLIGHTED_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/move_down_highlighted"));
    private static final Sprite MOVE_DOWN_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/move_down"));
    private static final Sprite ARROW_UP_SPRITE = Sprite.of16(ResourceUtil.getIcon("arrow_up"));
    private static final Sprite ARROW_DOWN_SPRITE = Sprite.of16(ResourceUtil.getIcon("arrow_down"));
    private static final Component REQUIRED = ResourceUtil.getText("profile.override.required");
    private static final Component FIXED_POSITION = ResourceUtil.getText("profile.override.fixed");
    private static final Component FIXED_TOP = ResourceUtil.getText("profile.override.fixed.top");
    private static final Component FIXED_BOTTOM = ResourceUtil.getText("profile.override.fixed.bottom");
    private static final Component REMOVE_OVERRIDES = ResourceUtil.getText("profile.override.remove");
    private static final Theme DROP_THEME = Theme.GREEN_500;
    private static final ColoredRect DROP_INDEX = new ColoredRect(DROP_THEME.getARGB());
    private static final GradientRect SCROLL_UP = GradientRect.fromTop(DROP_THEME.withAlpha(0.75f), DROP_THEME.withAlpha(0));
    private static final GradientRect SCROLL_DOWN = SCROLL_UP.flip();
    private static final int DEV_SPRITE_SIZE = 16;
    private static final int DEV_SPRITE_MARGIN_RIGHT = 8;
    private static final int DROP_INDEX_PADDING = 2;
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean keyPressed = super.keyPressed(keyCode, scanCode, modifiers);
        if (!keyPressed) {
            Entry entry = this.getEntry(this.getLastSelected());
            if (entry != null) {
                if (InputUtil.isMoveDown(keyCode, modifiers)) {
                    if (entry.moveDown()) playClickSound();
                    return true;
                } else if (InputUtil.isMoveUp(keyCode, modifiers)) {
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

    private int toPackIndex(int dropIndex) {
        List<Entry> children = this.children();

        if (dropIndex == -1) {
            return !children.isEmpty() ? this.packs.indexOf(this.children().getLast().getPack()) + 1 : -1;
        }

        return Math.clamp(this.packs.indexOf(this.children().get(dropIndex).getPack()), 0, this.packs.size());
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
        if (this.scrolling || this.isQueried() || this.isLocked() || payload.isEmpty() || (source != this && !(source instanceof AvailablePackList))) {
            return false;
        }

        List<Entry> children = this.children();
        if (children.isEmpty()) return true;

        int dropIndex = this.getDropIndex(mouseY);

        int minDropIndex = 0;
        int maxDropIndex = this.packs.size();
        for (int i = 0; i < this.packs.size(); i++) {
            Pack pack = this.packs.get(i);
            if (this.packAssets.isFixed(pack)) {
                switch (this.packAssets.getPosition(pack)) {
                    case TOP -> minDropIndex = i + 1;
                    case BOTTOM -> maxDropIndex = Math.min(i, maxDropIndex);
                }
            }
        }

        int dropPackIndex = this.toPackIndex(dropIndex);
        if (dropPackIndex == -1) dropPackIndex = minDropIndex;
        if (dropPackIndex < minDropIndex || dropPackIndex > maxDropIndex) {
            return false;
        }

        if (source != this) {
            return source.isTransferable(trigger);
        }
        if (this.packAssets.isFixed(trigger) || this.isMouserOverSelection(payload, mouseX, mouseY)) {
            return false;
        }

        int[] indices = this.getIndicesFromSelection(payload);
        if (indices.length == 0) return false;
        if (hasGap(indices)) return true;

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

        int dropPackIndex = this.toPackIndex(this.getDropIndex(mouseY));

        if (dropPackIndex == -1) {
            int minDropIndex = 0;
            for (int i = 0; i < this.packs.size(); i++) {
                Pack pack = this.packs.get(i);
                if (this.packAssets.isFixed(pack) && this.packAssets.getPosition(pack) == Pack.Position.TOP) {
                    minDropIndex = i + 1;
                }
            }
            dropPackIndex = minDropIndex;
        }

        if (source == this) {
            List<Pack> movable = new ObjectArrayList<>(payload);
            movable.removeIf(this.packAssets::isFixed);
            return this.moveAll(this.orderSelection(movable), dropPackIndex) ? payload : null;
        }

        this.clearSelection();
        List<Pack> dropped = new ObjectArrayList<>();
        for (Pack selected : payload) {
            if (source.isTransferable(selected)) {
                dropped.add(selected);
                this.insertDrop(selected, dropPackIndex);
                this.select(selected);
            }
        }
        source.removeAll(dropped);
        this.select(trigger);

        return dropped;
    }

    private void renderDropIndex(GuiGraphics guiGraphics, int mouseY, int x, int width) {
        int dropIndex = this.getDropIndex(mouseY);
        int rowTop = this.getRowTop(dropIndex != -1 ? dropIndex : this.children().size());
        int indexY = rowTop - this.rowGap - DROP_INDEX_PADDING;

        guiGraphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
        DROP_INDEX.render(guiGraphics, x, indexY, width, rowTop - indexY + DROP_INDEX_PADDING);
        guiGraphics.disableScissor();
    }

    @Override
    public void renderDroppableZone(GuiGraphics guiGraphics, PackList source, ImmutableList<Pack> payload, Pack trigger, int mouseX, int mouseY, float partialTick) {
        if (this.isLocked() || source.isLocked() || (source != this && source instanceof FolderPackList)) {
            return;
        }

        int x = this.getX();
        int y = this.getY();
        int width = this.scrollbarVisible() ? this.getWidth() - this.scrollbarOffset : this.getWidth();
        int height = this.getHeight();
        int bottom = this.getBottom();

        if (this.isMouseOver(mouseX, mouseY)) {
            double scrollAmount = this.scrollAmount();

            int scrollDownY = bottom - this.itemHeight;
            if (scrollAmount < this.maxScrollAmount() && mouseY >= scrollDownY) {
                SCROLL_DOWN.render(guiGraphics, x, scrollDownY, width, this.itemHeight);
                this.scrollStep(MoveDirection.DOWN, partialTick);
            } else if (scrollAmount > 0 && mouseY <= y + this.itemHeight) {
                SCROLL_UP.render(guiGraphics, x, y, width, this.itemHeight);
                this.scrollStep(MoveDirection.UP, partialTick);
            } else {
                this.scrolling = false;
            }

            if (this.canDrop(source, payload, trigger, mouseX, mouseY)) {
                this.renderDropIndex(guiGraphics, mouseY, x, width);
            }
        }

        guiGraphics.renderOutline(x, y, width, height, DROP_THEME.getARGB());
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
            return !CurrentPackList.this.packAssets.isRequired(this.pack) &&
                   !CurrentPackList.this.isLocked() &&
                   !this.isStale();
        }

        @Override
        protected boolean canDrag() {
            return !this.isFixed() && super.canDrag();
        }

        public boolean isFixed() {
            return CurrentPackList.this.packAssets.isFixed(this.pack) ||
                   CurrentPackList.this.isQueried() ||
                   CurrentPackList.this.isLocked() ||
                   this.isStale();
        }

        public boolean canMoveDown() {
            if (this.isFixed()) return false;

            int size = CurrentPackList.this.packs.size();
            if (this.isSelected()) {
                List<Pack> selection = CurrentPackList.this.getOrderedSelection().reversed();
                if (selection.size() > 1) {
                    Entry entry = CurrentPackList.this.getEntry(selection.getFirst());
                    int index = entry != null ? entry.getPackIndex() : -1;
                    int moveIndex = index > -1 ? entry.getMoveDownIndex() : -1;
                    return index > -1 &&
                           index < size - 1 &&
                           moveIndex > -1 &&
                           !CurrentPackList.this.packAssets.isFixed(CurrentPackList.this.packs.get(moveIndex));
                }
            }

            int index = this.getPackIndex();
            int moveIndex = this.getMoveDownIndex();
            return index > -1 &&
                   index < size - 1 &&
                   moveIndex > -1 &&
                   !CurrentPackList.this.packAssets.isFixed(CurrentPackList.this.packs.get(moveIndex));
        }

        public boolean canMoveUp() {
            if (this.isFixed()) return false;

            if (this.isSelected()) {
                List<Pack> selection = CurrentPackList.this.getOrderedSelection();
                if (selection.size() > 1) {
                    Entry entry = CurrentPackList.this.getEntry(selection.getFirst());
                    int index = entry != null ? entry.getPackIndex() : -1;
                    int moveIndex = index > -1 ? entry.getMoveUpIndex() : -1;
                    return index > 0 &&
                           moveIndex > -1 &&
                           !CurrentPackList.this.packAssets.isFixed(CurrentPackList.this.packs.get(moveIndex));
                }
            }

            int index = this.getPackIndex();
            int moveIndex = this.getMoveUpIndex();
            return index > 0 &&
                   moveIndex > -1 &&
                   !CurrentPackList.this.packAssets.isFixed(CurrentPackList.this.packs.get(moveIndex));
        }

        public boolean isMouseOverRemove(double mouseX, double mouseY) {
            return CurrentPackList.this.isHovered() && this.isTransferable() && GuiUtil.containsPoint(
                    this.getX() + SPACING,
                    this.getY(),
                    UNSELECT_SPRITE.width / 2,
                    UNSELECT_SPRITE.height,
                    mouseX,
                    mouseY
            );
        }

        public boolean isMouseOverUp(double mouseX, double mouseY) {
            return CurrentPackList.this.isHovered() && this.canMoveUp() && GuiUtil.containsPoint(
                    this.getX() + SPACING + MOVE_UP_SPRITE.width / 2,
                    this.getY(),
                    MOVE_UP_SPRITE.width / 2,
                    MOVE_UP_SPRITE.height / 2,
                    mouseX,
                    mouseY
            );
        }

        public boolean isMouseOverDown(double mouseX, double mouseY) {
            return CurrentPackList.this.isHovered() && this.canMoveDown() && GuiUtil.containsPoint(
                    this.getX() + SPACING + MOVE_DOWN_SPRITE.width / 2,
                    this.getY() + MOVE_DOWN_SPRITE.height / 2,
                    MOVE_DOWN_SPRITE.width / 2,
                    MOVE_DOWN_SPRITE.height / 2,
                    mouseX,
                    mouseY
            );
        }

        protected int getPackIndex() {
            return CurrentPackList.this.packs.indexOf(this.pack);
        }

        protected int getMoveUpIndex() {
            for (int i = this.getPackIndex() - 1; i >= 0; i--) {
                Pack nextPack = CurrentPackList.this.packs.get(i);
                if (CurrentPackList.this.packAssets.isFixed(nextPack)) {
                    return -1;
                }
                if (!CurrentPackList.this.packAssets.isHidden(nextPack)) {
                    return i;
                }
            }
            return -1;
        }

        protected int getMoveDownIndex() {
            for (int i = this.getPackIndex() + 1; i < CurrentPackList.this.packs.size(); i++) {
                Pack nextPack = CurrentPackList.this.packs.get(i);
                if (CurrentPackList.this.packAssets.isFixed(nextPack)) {
                    return -1;
                }
                if (!CurrentPackList.this.packAssets.isHidden(nextPack)) {
                    return i;
                }
            }
            return -1;
        }

        private @Nullable List<Pack> moveSelection(List<Pack> selection, ToIntFunction<Entry> indexGetter) {
            List<Pack> moved = new ObjectArrayList<>();

            Pack lastSelected = CurrentPackList.this.getLastSelected();
            for (int i = 0; i < selection.size(); i++) {
                Entry entry = CurrentPackList.this.getEntry(selection.get(i));
                int index = indexGetter.applyAsInt(entry);
                if (index > -1 && entry != null && CurrentPackList.this.move(selection.get(i), index)) {
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
            Entry entry = CurrentPackList.this.getEntry(this.pack);
            if (entry != null) CurrentPackList.this.ensureVisible(entry);
        }

        private boolean moveDirection(MoveDirection direction) {
            if ((direction.isUp() && !this.canMoveUp()) || (direction.isDown() && !this.canMoveDown())) {
                return false;
            }

            if (!this.isSelected()) {
                int packIndex = this.getPackIndex();
                if (packIndex > -1) {
                    int targetIndex = direction.isUp() ? this.getMoveUpIndex() : this.getMoveDownIndex();
                    if (targetIndex > -1 && CurrentPackList.this.move(this.pack, targetIndex)) {
                        CurrentPackList.this.selectExclusive(this.pack);
                        this.sendMoveEvent(List.of(this.pack));
                        return true;
                    }
                }
                return false;
            }

            List<Pack> moved = direction.isUp()
                    ? this.moveSelection(CurrentPackList.this.getOrderedSelection(), Entry::getMoveUpIndex)
                    : this.moveSelection(CurrentPackList.this.getOrderedSelection().reversed(), Entry::getMoveDownIndex);

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
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isLeftClick(button)) {
                if (this.isMouseOverRemove(mouseX, mouseY)) {
                    return this.consumeClick(CurrentPackList.Entry::transfer);
                } else if (this.isMouseOverUp(mouseX, mouseY)) {
                    return this.consumeClick(CurrentPackList.Entry::moveUp);
                } else if (this.isMouseOverDown(mouseX, mouseY)) {
                    return this.consumeClick(CurrentPackList.Entry::moveDown);
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        private boolean consumeClick(Consumer<CurrentPackList.Entry> action) {
            playClickSound();
            action.accept(this);
            return false;
        }

        @Override
        protected void renderForeground(GuiGraphics guiGraphics, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            if (!hovering && !this.isSelectedLast()) return;

            int x = left + SPACING;
            WHITE_OVERLAY.render(guiGraphics, x, top, UNSELECT_SPRITE.width, UNSELECT_SPRITE.height);

            if (this.isTransferable()) {
                pick(!this.isMouseOverRemove(mouseX, mouseY), UNSELECT_SPRITE, UNSELECT_HIGHLIGHTED_SPRITE).render(guiGraphics, x, top);
            }
            if (this.canMoveUp()) {
                pick(!this.isMouseOverUp(mouseX, mouseY), MOVE_UP_SPRITE, MOVE_UP_HIGHLIGHTED_SPRITE).render(guiGraphics, x, top);
            }
            if (this.canMoveDown()) {
                pick(!this.isMouseOverDown(mouseX, mouseY), MOVE_DOWN_SPRITE, MOVE_DOWN_HIGHLIGHTED_SPRITE).render(guiGraphics, x, top);
            }
        }

        @Override
        protected void renderDev(GuiGraphics guiGraphics, int top, int left) {
            if (PackedPacks.CONFIG.isDevMode()) {
                int size = DEV_SPRITE_SIZE;
                int iconX = (left + width) - size - DEV_SPRITE_MARGIN_RIGHT;

                PackOverride positionOverride = this.hasOverride(Profile::overridesPosition);
                if (positionOverride.booleanValue()) {
                    boolean fixedTop = CurrentPackList.this.packAssets.getPosition(this.pack) == Pack.Position.TOP;
                    guiGraphics.fill(iconX, top, iconX + size, top + size, positionOverride.backgroundColor());
                    pick(fixedTop, ARROW_UP_SPRITE, ARROW_DOWN_SPRITE).render(guiGraphics, iconX, top, size, size);
                    iconX -= size;
                }
                PackOverride requiredOverride = this.hasOverride(Profile::overridesRequired);
                if (requiredOverride.booleanValue()) {
                    boolean required = CurrentPackList.this.packAssets.isRequired(this.pack);
                    guiGraphics.fill(iconX, top, iconX + size, top + size, requiredOverride.backgroundColor());
                    pick(required, LOCK_SPRITE_SMALL, UNLOCK_SPRITE_SMALL).render(guiGraphics, iconX, top, size, size);
                    iconX -= size;
                }
                PackOverride hiddenOverride = this.hasOverride(Profile::isHidden);
                if (hiddenOverride.booleanValue()) {
                    guiGraphics.fill(iconX, top, iconX + size, top + size, hiddenOverride.backgroundColor());
                    EYE_SLASH_SPRITE.render(guiGraphics, iconX, top, size, size);
                }
            }
        }

        private void updateRequired(@Nullable Boolean required) {
            Profile profile = CurrentPackList.this.packAssets.getProfile();
            if (profile != null) {
                profile.setPacks(CurrentPackList.this.copyFlattenedPacks());
                profile.setRequired(required, this.selectionOrPack());
            }
        }

        private void updatePosition(@Nullable Pack.Position position) {
            Profile profile = CurrentPackList.this.packAssets.getProfile();
            if (profile != null) {
                profile.setPacks(CurrentPackList.this.copyFlattenedPacks());
                profile.setPosition(position, this.selectionOrPack());
            }
        }

        private void resetOverrides() {
            this.updateHidden(false);
            this.updateRequired(null);
            this.updatePosition(null);
        }

        private boolean isOptionActive(BiPredicate<Profile, Pack> option) {
            return !CurrentPackList.this.isLocked() && !this.isOverriddenByDefault(option);
        }

        private Sprite getIcon(boolean active, BiPredicate<Profile, Pack> defaultOption) {
            return this.isOverriddenByDefault(defaultOption) ? Sprite.of16(ResourceUtil.getIcon("radio_globe")) : getDefaultIcon(active);
        }

        @Override
        protected void onBuildHeader(ContextMenuItemBuilder builder) {
            Profile profile = PackedPacks.CONFIG.isDevMode() ? CurrentPackList.this.packAssets.getProfile() : null;
            if (profile == null) return;

            builder.add(devItem(HIDDEN)
                    .icon(() -> this.getIcon(profile.isHidden(this.pack), Profile::isHidden))
                    .activeWhen(() -> this.isOptionActive(Profile::isHidden))
                    .action(() -> this.updateHidden(!profile.isHidden(this.pack)))
                    .closeOnInteract(false)
                    .build());

            builder.add(devParent(REQUIRED)
                    .icon(() -> this.getIcon(profile.overridesRequired(this.pack), Profile::overridesRequired))
                    .activeWhen(() -> this.isOptionActive(Profile::overridesRequired) &&
                                      !this.pack.getId().equals(PackAssets.VANILLA_ID) &&
                                      !this.pack.getId().equals(PackAssets.FABRIC_ID))
                    .closeOnInteract(false)
                    .addChild(devItem(CommonComponents.OPTION_OFF)
                            .icon(() -> getDefaultIcon(!profile.overridesRequired(this.pack)))
                            .action(() -> this.updateRequired(null))
                            .closeOnInteract(false)
                            .build())
                    .addChild(devItem(CommonComponents.GUI_NO)
                            .icon(() -> getDefaultIcon(
                                    profile.overridesRequired(this.pack) &&
                                    !profile.isRequired(this.pack)
                            ))
                            .action(() -> this.updateRequired(false))
                            .closeOnInteract(false)
                            .build())
                    .addChild(devItem(CommonComponents.GUI_YES)
                            .icon(() -> getDefaultIcon(profile.isRequired(this.pack)))
                            .action(() -> this.updateRequired(true))
                            .closeOnInteract(false)
                            .build())
                    .build());

            builder.add(devParent(FIXED_POSITION)
                    .icon(() -> this.getIcon(profile.overridesPosition(this.pack), Profile::overridesPosition))
                    .activeWhen(() -> this.isOptionActive(Profile::overridesPosition))
                    .closeOnInteract(false)
                    .addChild(devItem(CommonComponents.OPTION_OFF)
                            .icon(() -> getDefaultIcon(!profile.overridesPosition(this.pack)))
                            .action(() -> this.updatePosition(null))
                            .closeOnInteract(false)
                            .build())
                    .addChild(devItem(FIXED_TOP)
                            .icon(() -> getDefaultIcon(profile.getPosition(this.pack) == Pack.Position.TOP))
                            .action(() -> this.updatePosition(Pack.Position.TOP))
                            .closeOnInteract(false)
                            .build())
                    .addChild(devItem(FIXED_BOTTOM)
                            .icon(() -> getDefaultIcon(profile.getPosition(this.pack) == Pack.Position.BOTTOM))
                            .action(() -> this.updatePosition(Pack.Position.BOTTOM))
                            .closeOnInteract(false)
                            .build())
                    .build());

            builder.add(devItem(REMOVE_OVERRIDES).action(this::resetOverrides).build()).separator();
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
