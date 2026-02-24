package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.ContextMenuEvent;
import io.github.fishstiz.packed_packs.api.events.InitializePackEntryEvent;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.MouseSelectionHandler;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.PackMenuHeader;
import io.github.fishstiz.packed_packs.gui.history.Restorable;
import io.github.fishstiz.packed_packs.gui.components.ToggleableHelper;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import io.github.fishstiz.packed_packs.impl.context.PackContextImpl;
import io.github.fishstiz.packed_packs.impl.events.ContextMenuEventImpl;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.PackFileOperations;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.gui.components.events.*;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
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
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;
import static io.github.fishstiz.fidgetz.util.lang.ObjectsUtil.*;

public abstract class PackList extends AbstractFixedListWidget<PackList.Entry> implements
        Restorable<PackList.Snapshot>,
        ContainerEventHandlerPatch,
        ContextMenuContainer {
    protected static final int Y_OFFSET = 1;
    protected static final int ITEM_HEIGHT = 35;
    protected static final int ROW_GAP = 3;
    protected final ScreenContext screenContext;
    protected final PackOptionsContext options;
    protected final PackAssetManager assets;
    protected final PackListModel list;
    private final PackFileOperations fileOps;
    private final ActionDispatcher dispatcher;

    protected PackList(PackListProps props) {
        super(ITEM_HEIGHT);
        this.assets = props.assets();
        this.options = props.options();
        this.screenContext = props.screenContext();
        this.dispatcher = props.dispatcher();
        this.fileOps = props.fileOps();
        this.list = new PackListModel(this.options);
    }

    protected abstract @NonNull Entry createEntry(PackContext context, int index);

    public @Nullable Entry getEntry(@Nullable Pack pack) {
        if (pack == null) return null;
        for (Entry entry : this.children()) {
            if (Objects.equals(entry.pack(), pack)) return entry;
        }
        return null;
    }

    private void refreshEntries() {
        Entry focused = this.getFocused();
        List<Pack> visiblePacks = this.list.getVisiblePacks();

        this.clearEntries();
        for (int i = 0; i < visiblePacks.size(); i++) {
            Pack pack = visiblePacks.get(i);
            PackContext context = new PackContextImpl(pack, this.assets, this.fileOps);
            Entry entry = this.createEntry(context, i);
            this.addEntry(entry);
        }

        this.clampScrollAmount();
        this.setFocused(mapOrNull(focused, f -> this.getEntry(f.pack())));
    }

    protected void refreshList() {
        this.list.refresh();
        this.refreshEntries();
    }

    public void scrollToTop() {
        this.setScrollAmount(0);
    }

    public void reload(Collection<Pack> packs) {
        this.list.replaceAll(packs);
        this.setFocused(null);
        this.refresh();
    }

    public @NonNull List<Pack> copyPacks() {
        return List.copyOf(this.list.getPacks());
    }

    public List<Pack> getOrderedSelection() {
        return this.list.getOrderedSelection();
    }

    public void clearSelection() {
        this.list.clearSelection();
    }

    private void refresh() {
        this.clearSelection();
        this.refreshList();
        this.scrollToTop();
    }

    public void sort(Query.SortOption sort) {
        if (this.list.sort(sort)) {
            this.refresh();
        }
    }

    public void hideIncompatible(boolean hideIncompatible) {
        if (this.list.hideIncompatible(hideIncompatible)) {
            this.clearSelection();
            this.refreshList();
        }
    }

    public void search(@NonNull String search) {
        if (this.list.search(search)) {
            this.refresh();
        }
    }

    public boolean isQueried() {
        return this.list.isQueried();
    }

    public void addAll(List<Pack> packs) {
        for (Pack pack : packs) {
            this.list.add(pack);
        }
        this.refreshList();
    }

    private void addOrMove(Pack pack, int to) {
        this.list.insertOrMove(to, pack);
        this.list.select(pack);
    }

    public void addAll(List<Pack> packs, int to) {
        for (Pack pack : packs) {
            this.addOrMove(pack, to);
        }
        this.refreshList();
    }

    public boolean moveAll(List<Pack> selection, int to) {
        if (this.list.moveAll(to, selection)) {
            this.refreshList();
            return true;
        }
        return true;
    }

    private boolean removePack(Pack pack) {
        Entry focused = this.getFocused();
        if (this.list.remove(pack)) {
            if (focused != null && focused.pack().getId().equals(pack.getId())) {
                this.setFocused(null);
            }
            return true;
        }
        return false;
    }

    public void remove(Pack pack) {
        this.removePack(pack);
        this.refreshList();
    }

    public void removeAll(List<Pack> packs) {
        boolean removed = false;
        for (Pack pack : packs) {
            removed |= this.removePack(pack);
        }
        if (removed) {
            this.refreshList();
        }
    }

    public @Nullable Pack getLastSelected() {
        return this.list.getLastSelected();
    }

    @Override
    public @Nullable Entry getSelected() {
        return this.getLastSelected() != null ? this.getEntry(this.getLastSelected()) : super.getSelected();
    }

    @Override
    public void setSelected(@Nullable Entry selected) {
        this.selected = selected;
    }

    public boolean isSelected(Pack pack) {
        return this.list.isSelected(pack);
    }

    public void scrollToLastSelected() {
        ifPresent(this.getEntry(this.getLastSelected()), this::scrollToEntry);
    }

    public void unselect(Pack pack) {
        this.list.unselect(pack);

        Entry entry = this.getEntry(pack);
        if (entry == this.getFocused()) {
            this.setFocused(null);
        }
        if (entry == this.getSelected()) {
            this.setSelected(null);
        }
    }

    public void select(Pack pack) {
        if (this.list.select(pack)) {
            Entry entry = this.getEntry(pack);
            this.setFocused(entry);
            this.setSelected(entry);
        }
    }

    public void selectAll() {
        this.list.getVisiblePacks().forEach(this::select);
    }

    public void selectAll(List<Pack> packs) {
        packs.forEach(this::select);
    }

    public void selectExclusive(Pack pack) {
        this.clearSelection();
        this.select(pack);
    }

    public void selectToggle(Pack pack) {
        if (this.isSelected(pack)) {
            this.unselect(pack);
        } else {
            this.select(pack);
        }
    }

    public void selectRange(Pack pack) {
        this.list.selectRange(pack);
        this.select(pack);
    }

    public boolean isTransferable(Pack pack) {
        return testNullable(this.getEntry(pack), PackList.Entry::isTransferable);
    }

    public void transferAll() {
        List<Pack> payload = new ArrayList<>();
        List<Pack> visiblePacks = this.list.getVisiblePacks();
        for (int i = visiblePacks.size() - 1; i >= 0; i--) {
            Pack pack = visiblePacks.get(i);
            if (this.isTransferable(pack)) {
                payload.add(pack);
            }
        }
        if (!payload.isEmpty()) {
            this.dispatch(new PackListAction.Transfer(this, this.getLastSelected(), payload));
        }
    }

    protected void dispatch(PackListAction action) {
        this.dispatcher.dispatch(action);
    }

    public abstract boolean canInteract(PackList source);

    protected abstract boolean canDrop(PackListAction.Drag dragged, double mouseX, double mouseY);

    protected abstract void handleDrop(PackListAction.Drag dragged, double mouseX, double mouseY);

    public abstract void renderDroppableZone(GuiGraphics guiGraphics, PackListAction.Drag dragged, int mouseX, int mouseY, float partialTick);

    public final void drop(PackListAction.Drag dragged, double mouseX, double mouseY) {
        if (!this.options.isLocked() && this.canDrop(dragged, mouseX, mouseY)) {
            this.handleDrop(dragged, mouseX, mouseY);
        }
    }

    protected void openFolder(FolderPack folderPack) {
        this.dispatch(new PackListAction.OpenFolder(this, folderPack));
    }

    private @Nullable ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation arrowNavigation) {
        Entry entry = switch (arrowNavigation.direction()) {
            case UP -> this.getPreviousEntry();
            case DOWN -> this.getNextEntry();
            default -> null;
        };
        if (entry != null) {
            if (isRangeModifierActive()) {
                this.selectRange(entry.pack());
            } else {
                this.selectExclusive(entry.pack());
            }
            this.dispatch(new PackListAction.Focus(this, null));
            this.scrollToEntry(entry);
            return ComponentPath.path(entry, this);
        }
        this.setFocused(null);
        return null;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) {
        if (!this.isFocused()) {
            Pack lastSelected = this.getLastSelected();
            Entry entry = null;
            if (lastSelected != null) {
                entry = this.getEntry(lastSelected);
            } else if (!this.children().isEmpty()) {
                entry = this.children().getFirst();
            }
            if (entry != null) {
                this.select(entry.pack());
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
        Entry entry = this.getEntry(this.getLastSelected());
        if (isExpandFolder(keyEvent) && entry != null && entry.folderWidget != null && this.list.getSelection().size() == 1) {
            this.openFolder(entry.folderWidget.getMetadata());
            return true;
        }
        if (isTransfer(keyEvent)) {
            if (entry != null && entry.transfer()) {
                playClickSound();
            }
            return entry != null;
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
        // for performance, return raw children instead of a new view
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

    public @NonNull Snapshot captureState(String eventName) {
        return new Snapshot(this);
    }

    @Override
    public void replaceState(@NonNull Snapshot snapshot) {
        snapshot.model.restore();
        this.refreshEntries();
        this.setFocused(this.getEntry(snapshot.focused));
        this.setSelected(this.getEntry(snapshot.selected));
    }

    public record Snapshot(
            PackList target,
            @Nullable Pack focused,
            @Nullable Pack selected,
            PackListModel.Snapshot model
    ) implements Restorable.Snapshot<Snapshot> {
        public Snapshot(PackList target, PackListModel.Snapshot model) {
            this(target, extractPack(target.getFocused()), extractPack(target.getSelected()), model);
        }

        public Snapshot(PackList target) {
            this(target, target.list.captureState());
        }

        public Snapshot replaceAll(List<Pack> packs) {
            return new Snapshot(this.target, this.focused, this.selected, this.model.replaceAll(packs));
        }

        public Snapshot retainAll(Set<Pack> packs) {
            return new Snapshot(this.target, this.focused, this.selected, this.model.retainAll(packs));
        }
    }

    private static @Nullable Pack extractPack(@Nullable Entry entry) {
        return mapOrNull(entry, Entry::pack);
    }

    public abstract class Entry extends AbstractFixedListWidget<Entry>.Entry implements ContainerEventHandlerPatch, ContextMenuContainer {
        private static final int V_MARGIN = ROW_GAP / 2 + Y_OFFSET;
        private static final int BACKGROUND_MARGIN = 1;
        private static final Tooltip FOLDER_OPEN_INFO = Tooltip.create(FolderPack.FOLDER_OPEN_TEXT);
        protected static final int H_SPACING = 2;
        protected static final ColoredRect SELECTED_OVERLAY = new ColoredRect(Theme.BLUE_500.withAlpha(0.25F));
        protected final PackContext context;
        private final MouseSelectionHandler<Pack> selectionHandler;
        private final List<GuiEventListener> children = new ObjectArrayList<>();
        private final List<Renderable> renderables = new ObjectArrayList<>();
        private final List<Renderable> topRenderables = new ObjectArrayList<>();
        private @Nullable PackListDevMenu devMenu;
        private @Nullable PackWidget packWidget;
        private @Nullable FidgetzButton<FolderPack> folderWidget;
        private boolean initialized;
        private boolean stale = false;

        protected Entry(PackContext context, int index) {
            super(index);
            this.context = context;
            this.selectionHandler = new MouseSelectionHandler<>(this, PackList.this.list.getSelection(), context.pack());
        }

        private void ensureInitialized() {
            if (!this.initialized) {
                this.init();
            }
        }

        private void init() {
            this.packWidget = this.addRenderableWidget(new PackWidget(
                    this.pack(),
                    PackList.this.assets,
                    this.getX(),
                    PackList.this.getRowTop(this.index),
                    this.getWidth(),
                    ITEM_HEIGHT - ROW_GAP,
                    H_SPACING
            ));

            boolean devMode = PackList.this.screenContext.devMode();

            if (this.pack() instanceof FolderPack folderPack && (devMode || Preferences.INSTANCE.folderPackWidget.get())) {
                this.folderWidget = this.addTopRenderableOnly(this.prependWidget(
                        ToggleableHelper.applyPref(Preferences.INSTANCE.folderPackWidget, FidgetzButton.<FolderPack>builder())
                                .setTooltip(FOLDER_OPEN_INFO)
                                .setHeight(this.packWidget.getHeight() / 3)
                                .makeSquare()
                                .setSprite(GuiConstants.HAMBURGER_SPRITE)
                                .setFocusOnInteract(false)
                                .setMetadata(folderPack)
                                .setOnPress(this::openFolder)
                                .build()));
            }

            if (devMode) {
                this.devMenu = new PackListDevMenu(PackList.this.minecraft, PackList.this.options, this);
            }

            this.initialized = true;

            InitializePackEntryEvent event = new InitializePackEntryEvent(PackList.this.screenContext, this.context, this, this::addTopLayer);
            PackedPacksApiImpl.getInstance().eventBus().post(event);
        }

        public Pack pack() {
            return this.context.pack();
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

        public boolean isTransferable() {
            return !PackList.this.options.isLocked() && !this.isStale();
        }

        public boolean isSelected() {
            return PackList.this.list.getSelection().contains(this.pack());
        }

        public boolean isSelectedLast() {
            List<Pack> selection = PackList.this.list.getSelection();
            return !selection.isEmpty() && Objects.equals(selection.getLast(), this.pack());
        }

        public List<Pack> getPackOrSelection() {
            return this.isSelected() ? List.copyOf(PackList.this.list.getSelection()) : List.of(this.pack());
        }

        protected void sendPacks(Pack trigger, List<Pack> payload) {
            PackList.this.dispatch(new PackListAction.Transfer(PackList.this, trigger, payload));
        }

        private boolean sendSelection() {
            List<Pack> payload = new ObjectArrayList<>();

            for (Pack selected : PackList.this.getOrderedSelection().reversed()) {
                if (PackList.this.isTransferable(selected)) {
                    payload.add(selected);
                }
            }

            if (!payload.isEmpty()) {
                Pack trigger = this.isTransferable() ? this.pack() : null;
                this.sendPacks(trigger, payload);
                return true;
            }

            return false;
        }

        public boolean transfer() {
            if (!this.isSelected() && this.isTransferable()) {
                PackList.this.dispatch(new PackListAction.Transfer(PackList.this, this.pack()));
                return true;
            }

            return this.sendSelection();
        }

        protected boolean handleMouseAction(MouseSelectionHandler.Action action) {
            if (!action.shouldDispatch() || this.isStale()) return false;

            switch (action) {
                case SELECT -> PackList.this.select(this.pack());
                case SELECT_TOGGLE -> PackList.this.selectToggle(this.pack());
                case SELECT_EXCLUSIVE -> PackList.this.selectExclusive(this.pack());
                case SELECT_RANGE -> PackList.this.selectRange(this.pack());
                case TRANSFER -> {
                    if (this.isTransferable()) {
                        PackList.this.dispatch(new PackListAction.Transfer(PackList.this, this.pack()));
                        return false;
                    }
                }
                case DRAG -> {
                    List<Pack> payload = PackList.this.getOrderedSelection().reversed();
                    PackList.this.dispatch(new PackListAction.Drag(PackList.this, this.pack(), payload));
                }
            }

            if (action.isSelection()) {
                PackList.this.dispatch(new PackListAction.Focus(PackList.this, this.pack()));
            }

            return true;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return PackList.this.beforeScrollbarX(mouseX) && super.isMouseOver(mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(@NonNull MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
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
            if (isOpenFile(keyEvent)) {
                PackUtil.openPack(this.pack());
                return true;
            }
            if (isOpenFolder(keyEvent)) {
                PackUtil.openParent(this.pack());
                return true;
            }
            if (isDelete(keyEvent) && this.canOperateFile()) {
                this.deletePack();
                return true;
            }
            if (isRename(keyEvent) && this.canOperateFile()) {
                this.renamePack();
                return true;
            }
            return false;
        }

        public void renderBack(GuiGraphics guiGraphics, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            if (!this.pack().getCompatibility().isCompatible() && !PackList.this.options.getUserConfig().isIncompatibleWarningsHidden()) {
                int backgroundLeft = left + BACKGROUND_MARGIN;
                int backgroundTop = top + BACKGROUND_MARGIN;
                int backgroundRight = backgroundLeft + width - BACKGROUND_MARGIN * 2;
                int backgroundBottom = backgroundTop + height - BACKGROUND_MARGIN;

                guiGraphics.fill(backgroundLeft, backgroundTop, backgroundRight, backgroundBottom, Theme.RED_900.getARGB());
            }
        }

        protected abstract void renderForeground(GuiGraphics guiGraphics, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick);

        private void renderSelection(GuiGraphics guiGraphics, int top, int left, int width, int height) {
            if (this.isSelected()) {
                pick(isSelectedLast(), WHITE_OVERLAY, SELECTED_OVERLAY).render(guiGraphics, left, top, width, height);
                DrawUtil.renderOutline(guiGraphics, left, top, width, height, Theme.BLUE_500.getARGB());
            }
        }

        protected void renderTop(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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

            this.renderBack(guiGraphics, top, left, width, height, mouseX, mouseY, hovering, partialTick);

            for (Renderable renderable : this.renderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            this.renderSelection(guiGraphics, top, left, width, height + Y_OFFSET);
            this.renderForeground(guiGraphics, innerTop, left, width, innerHeight, mouseX, mouseY, hovering, partialTick);
            this.renderTop(guiGraphics, mouseX, mouseY, partialTick);

            if (this.devMenu != null) {
                this.devMenu.renderDevSprites(guiGraphics, innerTop, left, width);
            }
        }

        protected void handleDevMenuEvent(PackListDevMenu.Event<?> event) {
            if (event instanceof PackListDevMenu.Event.EditAliases editAliases) {
                PackList.this.dispatch(new PackListAction.OpenAliases(PackList.this, editAliases.trigger()));
            }
        }

        @Override
        public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
            if (!this.initialized) return;

            PackList.this.setFocused(this);

            var extensions = ContextMenuEventImpl.postPackEntry(PackList.this.screenContext, this.context);

            ContextMenuContainer.super.buildItems(builder
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.BEFORE_HEADER))
                            .ifTrue((items, b) -> b.addAll(items))
                            .add(new PackMenuHeader(this.pack(), this.packWidget.getSprite()))
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.AFTER_HEADER))
                            .ifTrue((items, b) -> b.addAll(items))
                            .whenNonNull(this.devMenu)
                            .ifTrue(PackListDevMenu::onBuildHeader)
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.AFTER_DEV))
                            .ifTrue((items, b) -> b.addAll(items))
                            .whenNonNull(this.folderWidget)
                            .ifTrue(b -> b
                                    .simpleItem(FolderPack.FOLDER_OPEN_TEXT, this::openFolder)
                                    .separator()
                            )
                            .whenNonNull(((FilePack) this.pack()).packed_packs$getPath())
                            .ifTrue(b -> b
                                    .simpleItem(RENAME_FILE_TEXT, this::canOperateFile, this::renamePack)
                                    .simpleItem(DELETE_FILE_TEXT, this::canOperateFile, this::deletePack)
                                    .simpleItem(OPEN_FILE_TEXT, () -> PackUtil.openPack(this.pack()))
                                    .simpleItem(OPEN_PARENT_TEXT, () -> PackUtil.openParent(this.pack()))
                            )
                            .whenNonNull(extensions.getItems(ContextMenuEvent.PackEntry.Pos.AFTER_HEADER))
                            .ifTrue((items, b) -> b.addAll(items)),
                    mouseX,
                    mouseY
            );
        }

        private void openFolder() {
            PackList.this.openFolder(Objects.requireNonNull(this.folderWidget, "Cannot open folder without folder widget").getMetadata());
        }

        public boolean canOperateFile() {
            return PackList.this.fileOps.isOperable(this.pack());
        }

        public void deletePack() {
            PackList.this.dispatch(new PackListAction.Delete(PackList.this, this.pack()));
        }

        public void renamePack() {
            PackList.this.dispatch(new PackListAction.OpenRename(PackList.this, this.pack()));
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
