package io.github.fishstiz.packed_packs.gui.components.pack;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.GradientRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.GuiSprite;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializePackEntryEvent;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.MouseSelectionHandler;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.PackMenuHeader;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.impl.events.ContextMenuEventImpl;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static io.github.fishstiz.fidgetz.util.GuiUtil.playClickSound;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.ResourceUtil.getVanilla;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;
import static io.github.fishstiz.fidgetz.util.lang.ObjectsUtil.*;

public class PackList extends AbstractFixedListWidget<PackList.Entry> implements ContainerEventHandlerPatch, ContextMenuContainer {
    private static final int Y_OFFSET = 1;
    private static final int ITEM_HEIGHT = 35;
    private static final int ROW_GAP = 3;
    private static final double SCROLL_STEP = 10;
    private static final int DROP_INDEX_PADDING = 3;
    private final ScreenContext screenContext;
    private final PackListViewModel viewModel;
    private Theme dropTheme;
    private ColoredRect dropRect;
    private ColoredRect dropIndexRect;
    private GradientRect scrollUpRect;
    private GradientRect scrollDownRect;
    private boolean scrolling;

    public PackList(ScreenContext screenContext, PackListViewModel viewModel) {
        super(ITEM_HEIGHT);
        this.screenContext = screenContext;
        this.viewModel = viewModel;
        this.applyTheme();
        this.refresh();
    }

    private void applyTheme() {
        if (this.viewModel.canReorder()) {
            this.dropTheme = Theme.GREEN_500;
            this.dropIndexRect = new ColoredRect(dropTheme.getARGB());
            this.scrollUpRect = GradientRect.fromTop(dropTheme.withAlpha(0.75f), dropTheme.withAlpha(0));
            this.scrollDownRect = this.scrollUpRect.flip();
        } else {
            this.dropTheme = Theme.RED_700;
            this.dropRect = new ColoredRect(dropTheme.withAlpha(0.25f));
        }
    }

    public @Nullable Entry getEntry(@Nullable Pack pack) {
        if (pack == null) return null;
        for (Entry entry : this.children()) {
            if (Objects.equals(entry.pack(), pack)) return entry;
        }
        return null;
    }

    public void refresh() {
        List<PackListViewModel.Entry> entries = this.viewModel.entries();
        Entry focused = this.getFocused();

        this.clearEntries();
        for (int i = 0; i < entries.size(); i++) {
            this.addEntry(new Entry(entries.get(i), i));
        }

        this.clampScrollAmount();
        this.setFocused(mapOrNull(focused, f -> this.getEntry(f.pack())));
    }

    public void scrollToTop() {
        this.setScrollAmount(0);
    }

    private @Nullable Entry getLastSelected() {
        for (Entry entry : this.children()) {
            if (entry.viewModel.selectedLast()) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public @Nullable Entry getSelected() {
        Entry lastSelected = this.getLastSelected();
        return lastSelected != null ? lastSelected : super.getSelected();
    }

    @Override
    public void setSelected(@Nullable Entry selected) {
        this.selected = selected;
    }

    public void scrollToLastSelected() {
        ifPresent(this.getLastSelected(), this::scrollToEntry);
    }

    private void scrollStep(boolean up, float partialTick) {
        double scrollAmount = this.scrollAmount();
        if (up) {
            scrollAmount -= SCROLL_STEP * partialTick;
        } else {
            scrollAmount += SCROLL_STEP * partialTick;
        }

        this.scrolling = true;
        this.setClampedScrollAmount(scrollAmount);
    }

    public int getDropIndex(double mouseY) {
        if (this.children().isEmpty()) return -1;

        int index = this.getRowIndex(mouseY);
        if (index == -1) return -1;

        PackList.Entry entry = this.getEntry(index);
        int centerY = entry.getY() + (entry.getHeight() / 2);

        if (mouseY >= centerY) {
            int next = index + 1;
            return next < this.children().size() ? next : -1;
        }
        return index;
    }

    private boolean isMouserOverSelection(List<Pack> selection, double mouseX, double mouseY) {
        for (Pack selected : selection) {
            PackList.Entry entry = this.getEntry(selected);
            if (entry != null && entry.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    public boolean isDropWithinBounds(PackListViewModel source, Pack pack, List<Pack> payload, int mouseX, int mouseY, int index) {
        if (this.scrolling || (source == this.viewModel && this.isMouserOverSelection(payload, mouseX, mouseY))) {
            return false;
        }
        return this.viewModel.canAccept(source, pack, payload, index);
    }

    private void renderDropIndex(GuiGraphics guiGraphics, int x, int width, int index) {
        int rowTop = Math.clamp(
                this.getRowTop(index != -1 ? index : this.children().size()),
                this.getY() + this.offsetY + DROP_INDEX_PADDING,
                this.getBottom() - this.rowGap - DROP_INDEX_PADDING
        );
        int indexY = rowTop - this.rowGap - DROP_INDEX_PADDING;

        guiGraphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
        this.dropIndexRect.render(guiGraphics, x, indexY, width, rowTop - indexY + DROP_INDEX_PADDING);
        guiGraphics.disableScissor();
    }

    private void renderDroppableSlots(GuiGraphics guiGraphics, PackListViewModel source, Pack pack, List<Pack> payload, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        int width = this.scrollbarVisible() ? this.getWidth() - this.scrollbarOffset : this.getWidth();
        int height = this.getHeight();
        int bottom = this.getBottom();

        if (this.isMouseOver(mouseX, mouseY)) {
            double scrollAmount = this.scrollAmount();

            int scrollDownY = bottom - this.getItemHeight();
            if (scrollAmount < this.maxScrollAmount() && mouseY >= scrollDownY) {
                this.scrollDownRect.render(guiGraphics, x, scrollDownY, width, this.getItemHeight());
                this.scrollStep(false, partialTick);
            } else if (scrollAmount > 0 && mouseY <= y + this.getItemHeight()) {
                this.scrollUpRect.render(guiGraphics, x, y, width, this.getItemHeight());
                this.scrollStep(true, partialTick);
            } else {
                this.scrolling = false;
            }

            int index = this.getDropIndex(mouseY);
            if (this.isDropWithinBounds(source, pack, payload, mouseX, mouseY, index)) {
                this.renderDropIndex(guiGraphics, x, width, index);
            }
        }

        DrawUtil.renderOutline(guiGraphics, x, y, width, height, dropTheme.getARGB());
    }

    private void renderDroppableRect(GuiGraphics guiGraphics, PackListViewModel source, Pack pack, List<Pack> payload, int mouseX, int mouseY, float partialTick) {
        if (this.isDropWithinBounds(source, pack, payload, mouseX, mouseY, 0)) {
            if (this.isMouseOver(mouseX, mouseY)) {
                this.dropRect.render(guiGraphics, this.getX(), this.getY(), width, this.getHeight(), partialTick);
            }

            DrawUtil.renderOutline(guiGraphics, this.getX(), this.getY(), width, this.getHeight(), this.dropTheme.getARGB());
        }
    }

    public void renderDroppableZone(GuiGraphics guiGraphics, PackListViewModel source, Pack pack, List<Pack> payload, int mouseX, int mouseY, float partialTick) {
        if (!this.viewModel.locked() && source.canInteract(this.viewModel)) {
            if (this.viewModel.canReorder()) {
                this.renderDroppableSlots(guiGraphics, source, pack, payload, mouseX, mouseY, partialTick);
            } else {
                this.renderDroppableRect(guiGraphics, source, pack, payload, mouseX, mouseY, partialTick);
            }
        }
    }

    private @Nullable ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation arrowNavigation) {
        Entry entry = switch (arrowNavigation.direction()) {
            case UP -> this.getPreviousEntry();
            case DOWN -> this.getNextEntry();
            default -> null;
        };
        if (entry != null) {
            if (isRangeModifierActive()) {
                entry.viewModel.selectRange();
            } else {
                entry.viewModel.selectExclusive();
            }
            entry.viewModel.select();
            this.scrollToEntry(entry);
            return ComponentPath.path(entry, this);
        }
        this.setFocused(null);
        return null;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) {
        if (!this.isFocused()) {
            Entry entry = this.getLastSelected();
            if (entry != null && !this.children().isEmpty()) {
                entry = this.children().getFirst();
            }
            if (entry != null) {
                entry.viewModel.select();
                this.scrollToEntry(entry);
                return ComponentPath.path(entry, this);
            }
        } else if (event instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation) {
            return this.handleArrowNavigation(arrowNavigation);
        } else {
            this.setFocused(null);
        }
        return null;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        Entry entry = this.getLastSelected();
        if (isExpandFolder(keyEvent) && entry != null && entry.viewModel.folder().isPresent() && entry.viewModel.selectedExclusive()) {
            entry.viewModel.openFolder();
            return true;
        }
        if (isTransfer(keyEvent)) {
            if (entry != null && !entry.isStale() && entry.viewModel.canMoveOut()) {
                entry.viewModel.transfer();
                playClickSound();
            }
            return entry != null;
        }
        if (isMoveDown(keyEvent)) {
            if (entry != null && !entry.isStale() && entry.viewModel.canMoveDown()) {
                entry.viewModel.moveDown();
                this.scrollToEntry(entry);
                playClickSound();
            }
            return true;
        } else if (isMoveUp(keyEvent)) {
            if (entry != null && !entry.isStale() && entry.viewModel.canMoveUp()) {
                entry.viewModel.moveUp();
                this.scrollToEntry(entry);
                playClickSound();
            }
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        boolean scrolling = this.updateScrolling(mouseButtonEvent);
        return this.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y()) &&
               ContainerEventHandlerPatch.super.mouseClickedAt(mouseButtonEvent, doubleClicked) ||
               scrolling;
    }

    @Override
    public @NonNull List<Entry> children() {
        return this.children;
    }

    @Override
    protected void renderListItems(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderListItems(guiGraphics, mouseX, mouseY, partialTick);

        Entry focused = this.getFocused();
        if (focused != null && focused.isFocused() && this.children().contains(focused)) {
            int outlineTop = focused.getY();
            int outlineHeight = focused.getHeight() + Y_OFFSET;
            DrawUtil.renderOutline(guiGraphics, focused.getX(), outlineTop, focused.getWidth(), outlineHeight, Theme.WHITE.getARGB());
        }
    }

    @Override
    protected void renderItem(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, Entry item) {
        item.ensureInitialized();
        super.renderItem(guiGraphics, mouseX, mouseY, partialTick, item);
    }

    @Override
    public int maxScrollAmount() {
        int maxScrollAmount = super.maxScrollAmount();
        return maxScrollAmount > 0 ? maxScrollAmount + Y_OFFSET : maxScrollAmount;
    }

    public class Entry extends AbstractFixedListWidget<Entry>.Entry implements SelectableEntry, ContainerEventHandlerPatch, ContextMenuContainer {
        private static final int V_MARGIN = ROW_GAP / 2 + Y_OFFSET;
        private static final int BACKGROUND_MARGIN = 1;
        private static final int H_SPACING = 2;
        private static final int ICON_SIZE = ITEM_HEIGHT - ROW_GAP;
        private static final Tooltip FOLDER_OPEN_INFO = Tooltip.create(FolderPack.FOLDER_OPEN_TEXT);
        private static final ColoredRect SELECTED_OVERLAY = new ColoredRect(Theme.BLUE_500.withAlpha(0.25F));
        private static final Sprite SELECT_HIGHLIGHTED_SPRITE = GuiSprite.of32(getVanilla("transferable_list/select_highlighted"));
        private static final Sprite SELECT_SPRITE = GuiSprite.of32(getVanilla("transferable_list/select"));
        private static final Sprite UNSELECT_HIGHLIGHTED_SPRITE = GuiSprite.of32(getVanilla("transferable_list/unselect_highlighted"));
        private static final Sprite UNSELECT_SPRITE = GuiSprite.of32(getVanilla("transferable_list/unselect"));
        private static final Sprite MOVE_UP_HIGHLIGHTED_SPRITE = GuiSprite.of32(getVanilla("transferable_list/move_up_highlighted"));
        private static final Sprite MOVE_UP_SPRITE = GuiSprite.of32(getVanilla("transferable_list/move_up"));
        private static final Sprite MOVE_DOWN_HIGHLIGHTED_SPRITE = GuiSprite.of32(getVanilla("transferable_list/move_down_highlighted"));
        private static final Sprite MOVE_DOWN_SPRITE = GuiSprite.of32(getVanilla("transferable_list/move_down"));
        private final PackListViewModel.Entry viewModel;
        private final MouseSelectionHandler<Pack> selectionHandler;
        private final List<GuiEventListener> children = new ObjectArrayList<>();
        private final List<Renderable> renderables = new ObjectArrayList<>();
        private final List<Renderable> topRenderables = new ObjectArrayList<>();
        private @Nullable PackListDevMenu devMenu;
        private @Nullable PackWidget packWidget;
        private @Nullable FidgetzButton<Void> folderWidget;
        private boolean initialized;
        private boolean stale = false;

        Entry(PackListViewModel.Entry viewModel, int index) {
            super(index);
            this.viewModel = viewModel;
            this.selectionHandler = new MouseSelectionHandler<>(this, viewModel::selected, viewModel::selectedLast, viewModel::selectedExclusive);
        }

        private void ensureInitialized() {
            if (!this.initialized) {
                this.init();
            }
        }

        private void init() {
            this.packWidget = this.addRenderableWidget(new PackWidget(this.viewModel, ITEM_HEIGHT - ROW_GAP, H_SPACING));

            boolean devMode = PackList.this.screenContext.devMode();

            if (this.viewModel.folder().isPresent() && (devMode || Preferences.INSTANCE.folderPackWidget.get())) {
                this.folderWidget = this.addTopRenderableOnly(this.prependWidget(
                        ToggleableHelper.applyPref(Preferences.INSTANCE.folderPackWidget, FidgetzButton.<Void>builder())
                                .setTooltip(FOLDER_OPEN_INFO)
                                .setHeight(this.packWidget.getHeight() / 3)
                                .makeSquare()
                                .setSprite(GuiConstants.HAMBURGER_SPRITE)
                                .setFocusOnInteract(false)
                                .setOnPress(this.viewModel::openFolder)
                                .build()));
            }

            if (devMode) {
                this.devMenu = this.viewModel.devMenu(PackList.this.minecraft);
            }

            this.initialized = true;

            InitializePackEntryEvent event = new InitializePackEntryEvent(PackList.this.screenContext, this.viewModel, this, this::addTopLayer);
            PackedPacksApiImpl.getInstance().eventBus().post(event);
        }

        public Pack pack() {
            return this.viewModel.pack();
        }

        public <U extends GuiEventListener & Renderable> U addRenderableWidget(U widget) {
            this.children.add(widget);
            this.renderables.add(widget);
            return widget;
        }

        public <U extends GuiEventListener> U prependWidget(U widget) {
            this.children.addFirst(widget);
            return widget;
        }

        public <U extends Renderable> U addTopRenderableOnly(U renderable) {
            this.topRenderables.add(renderable);
            return renderable;
        }

        public <U extends GuiEventListener & Renderable> void addTopLayer(U widget) {
            this.addTopRenderableOnly(this.prependWidget(widget));
        }

        private boolean handleMouseAction(MouseSelectionHandler.Action action) {
            if (!action.shouldDispatch() || this.isStale()) return false;

            switch (action) {
                case SELECT -> this.viewModel.select();
                case SELECT_TOGGLE -> this.viewModel.selectToggle();
                case SELECT_EXCLUSIVE -> this.viewModel.selectExclusive();
                case SELECT_RANGE -> this.viewModel.selectRange();
                case DRAG -> this.viewModel.drag();
                case TRANSFER -> {
                    this.viewModel.transfer();
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return PackList.this.beforeScrollbarX(mouseX) && super.isMouseOver(mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(@NonNull MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
            if (isLeftClick(mouseButtonEvent) && !this.isStale()) {
                int relativeX = (int) mouseButtonEvent.x() - this.getX() + H_SPACING;
                int relativeY = (int) mouseButtonEvent.y() - this.getY();

                if (this.viewModel.canEnable() && this.mouseOverIcon(relativeX, relativeY, ICON_SIZE)) {
                    this.viewModel.enable();
                    playClickSound();
                    return false;
                }

                if (this.viewModel.canDisable() && this.mouseOverLeftHalf(relativeX, relativeY, ICON_SIZE)) {
                    this.viewModel.disable();
                    playClickSound();
                    return false;
                }

                if (this.viewModel.canMoveUp() && this.mouseOverTopRightQuarter(relativeX, relativeY, ICON_SIZE)) {
                    this.viewModel.moveUp();
                    playClickSound();
                    return false;
                }

                if (this.viewModel.canMoveDown() && this.mouseOverBottomRightQuarter(relativeX, relativeY, ICON_SIZE)) {
                    this.viewModel.moveDown();
                    playClickSound();
                    return false;
                }
            }

            if (ContainerEventHandlerPatch.super.mouseClicked(mouseButtonEvent, doubleClicked)) {
                return false;
            }

            return this.handleMouseAction(this.selectionHandler.mouseClicked(mouseButtonEvent));
        }

        @Override
        public boolean mouseReleased(@NonNull MouseButtonEvent mouseButtonEvent) {
            return this.handleMouseAction(this.selectionHandler.mouseReleased(mouseButtonEvent));
        }

        @Override
        public boolean mouseDragged(@NonNull MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
            return this.handleMouseAction(this.selectionHandler.mouseDragged(mouseButtonEvent, dragX, dragY));
        }

        @Override
        public boolean keyPressed(@NonNull KeyEvent keyEvent) {
            if (super.keyPressed(keyEvent)) {
                return true;
            }
            if (this.isStale()) {
                return false;
            }
            if (isOpenFile(keyEvent)) {
                PackUtil.openPack(this.pack());
                return true;
            }
            if (isOpenFolder(keyEvent)) {
                PackUtil.openParent(this.pack());
                return true;
            }
            if (isDelete(keyEvent) && this.viewModel.fileModifiable()) {
                this.viewModel.delete();
                return true;
            }
            if (isRename(keyEvent) && this.viewModel.fileModifiable()) {
                this.viewModel.openRename();
                return true;
            }
            return false;
        }

        public void renderBack(GuiGraphics guiGraphics, int top, int left, int width, int height) {
            if (!this.pack().getCompatibility().isCompatible() && !this.viewModel.incompatibleWarningsHidden()) {
                int backgroundLeft = left + BACKGROUND_MARGIN;
                int backgroundTop = top + BACKGROUND_MARGIN;
                int backgroundRight = backgroundLeft + width - BACKGROUND_MARGIN * 2;
                int backgroundBottom = backgroundTop + height - BACKGROUND_MARGIN;

                guiGraphics.fill(backgroundLeft, backgroundTop, backgroundRight, backgroundBottom, Theme.RED_900.getARGB());
            }
        }

        private void updateCursor(GuiGraphics guiGraphics, boolean hovered) {
            if (hovered) {
                guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        }

        private void renderForeground(GuiGraphics guiGraphics, int top, int left, boolean hovering) {
            if (!hovering || !this.viewModel.selectedLast()) return;

            int x = left + H_SPACING;
            int relativeX = x - this.getX();
            int relativeY = top - this.getY();

            WHITE_OVERLAY.render(guiGraphics, x, top, ICON_SIZE, ICON_SIZE);

            if (this.viewModel.canEnable()) {
                boolean hovered = this.mouseOverIcon(relativeX, relativeY, ICON_SIZE);
                pick(hovered, SELECT_HIGHLIGHTED_SPRITE, SELECT_SPRITE).render(guiGraphics, x, top);
                this.updateCursor(guiGraphics, hovered);
            }

            if (this.viewModel.canDisable()) {
                boolean hovered = this.mouseOverLeftHalf(relativeX, relativeY, ICON_SIZE);
                pick(hovered, UNSELECT_HIGHLIGHTED_SPRITE, UNSELECT_SPRITE).render(guiGraphics, x, top);
                this.updateCursor(guiGraphics, hovered);
            }

            if (this.viewModel.canMoveUp()) {
                boolean hovered = this.mouseOverTopRightQuarter(relativeX, relativeY, ICON_SIZE);
                pick(hovered, MOVE_UP_HIGHLIGHTED_SPRITE, MOVE_UP_SPRITE).render(guiGraphics, x, top);
                this.updateCursor(guiGraphics, hovered);
            }

            if (this.viewModel.canMoveDown()) {
                boolean hovered = this.mouseOverBottomRightQuarter(relativeX, relativeY, ICON_SIZE);
                pick(hovered, MOVE_DOWN_HIGHLIGHTED_SPRITE, MOVE_DOWN_SPRITE).render(guiGraphics, x, top);
                this.updateCursor(guiGraphics, hovered);
            }
        }

        private void renderSelection(GuiGraphics guiGraphics, int top, int left, int width, int height) {
            if (this.viewModel.selected()) {
                pick(this.viewModel.selectedLast(), WHITE_OVERLAY, SELECTED_OVERLAY).render(guiGraphics, left, top, width, height);
                DrawUtil.renderOutline(guiGraphics, left, top, width, height, Theme.BLUE_500.getARGB());
            }
        }

        private void renderTop(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (this.folderWidget != null) {
                int folderWidgetY = this.getBottom() - this.folderWidget.getHeight() - BACKGROUND_MARGIN;
                this.folderWidget.setPosition(this.packWidget.getContentLeft(), folderWidgetY);
            }

            for (Renderable renderable : this.topRenderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public void renderContent(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            hovering = hovering && PackList.this.beforeScrollbarX(mouseX) && GuiUtil.isHovered(this, mouseX, mouseY);

            int left = this.getX();
            int top = this.getY();
            int width = this.getWidth();
            int height = this.getHeight();
            int innerTop = top + V_MARGIN;
            int innerHeight = height - ROW_GAP;

            this.packWidget.setPosition(left, innerTop);
            this.packWidget.setWidth(width);

            this.renderBack(guiGraphics, top, left, width, height);

            for (Renderable renderable : this.renderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            this.renderSelection(guiGraphics, top, left, width, height + Y_OFFSET);
            this.renderForeground(guiGraphics, innerTop, left, hovering);
            this.renderTop(guiGraphics, mouseX, mouseY, partialTick);

            if (this.devMenu != null) {
                this.devMenu.render(guiGraphics, innerTop, left, width, innerHeight, partialTick);
            }
        }

        @Override
        public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
            if (!this.initialized) return;

            PackList.this.setFocused(this);

            var extensions = ContextMenuEventImpl.postPackEntry(PackList.this.screenContext, this.viewModel);

            ContextMenuContainer.super.buildItems(builder
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.BEFORE_HEADER))
                            .ifTrue((items, b) -> b.addAll(items))
                            .add(new PackMenuHeader(this.pack(), this.viewModel.sprite()))
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.AFTER_HEADER))
                            .ifTrue((items, b) -> b.addAll(items))
                            .whenNonNull(this.devMenu)
                            .ifTrue((menu, b) -> menu.buildItems(b, mouseX, mouseY))
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.AFTER_DEV))
                            .ifTrue((items, b) -> b.addAll(items))
                            .whenNonNull(this.folderWidget)
                            .ifTrue(b -> b
                                    .simpleItem(FolderPack.FOLDER_OPEN_TEXT, this.viewModel::openFolder)
                                    .separator()
                            )
                            .when(this.viewModel.fileModifiable())
                            .ifTrue(b -> b
                                    .simpleItem(RENAME_FILE_TEXT, this.viewModel::fileModifiable, this.viewModel::openRename)
                                    .simpleItem(DELETE_FILE_TEXT, this.viewModel::fileModifiable, this.viewModel::delete)
                                    .simpleItem(OPEN_FILE_TEXT, () -> PackUtil.openPack(this.pack()))
                                    .simpleItem(OPEN_PARENT_TEXT, () -> PackUtil.openParent(this.pack()))
                            )
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.AFTER_HEADER))
                            .ifTrue((items, b) -> b.addAll(items)),
                    mouseX,
                    mouseY
            );
        }

        public void onDelete() {
            this.stale = true;
        }

        public void onRename(Component newName) {
            this.stale = true;

            if (this.packWidget != null) {
                this.packWidget.onRename(newName);
            }
            if (this.folderWidget != null) {
                this.folderWidget.active = false;
            }
        }

        public boolean isStale() {
            return this.stale;
        }

        @Override
        public @NonNull List<GuiEventListener> children() {
            return this.children;
        }

        @Override
        public @NonNull List<NarratableEntry> narratables() {
            return Collections.emptyList();
        }
    }
}
