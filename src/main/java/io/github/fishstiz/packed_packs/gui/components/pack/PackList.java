package io.github.fishstiz.packed_packs.gui.components.pack;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.ModAdditions;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.components.MouseSelectionHandler;
import io.github.fishstiz.packed_packs.gui.components.SelectionContext;
import io.github.fishstiz.packed_packs.gui.components.contextmenu.PackMenuHeader;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.gui.history.Restorable;
import io.github.fishstiz.packed_packs.gui.metadata.Toggleable;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.PackFileOperations;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.transform.mixin.gui.AbstractSelectionListAccessor;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ToastUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.gui.components.events.*;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.util.lang.CollectionsUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.google.common.primitives.Ints.contains;
import static io.github.fishstiz.fidgetz.util.GuiUtil.playClickSound;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;
import static io.github.fishstiz.packed_packs.util.lang.IntsUtil.hasGap;
import static io.github.fishstiz.packed_packs.util.lang.ObjectsUtil.*;

public abstract class PackList extends AbstractDynamicList<PackList.Entry> implements
        Restorable<PackList.Snapshot>,
        ContainerEventHandlerPatch,
        ContextMenuContainer {
    protected static final int OFFSET_Y = 2;
    protected static final int ITEM_HEIGHT = 32;
    protected static final int ROW_GAP = 3;
    protected final PackOptionsContext options;
    protected final PackAssetManager assets;
    protected final PackFileOperations fileOps;
    protected final List<Pack> packs = new ObjectArrayList<>();
    private final List<Pack> queried = new ObjectArrayList<>();
    private final List<Pack> selection = new ObjectArrayList<>();
    private final PackListEventListener listener;
    private final Query query;

    protected PackList(PackOptionsContext options, PackAssetManager assets, PackFileOperations fileOps, PackListEventListener listener) {
        super(ITEM_HEIGHT, DEFAULT_SCROLLBAR_OFFSET, OFFSET_Y, ROW_GAP);
        this.options = options;
        this.assets = assets;
        this.listener = listener;
        this.fileOps = fileOps;
        this.query = new Query();
        this.queryPacks();
    }

    protected abstract @NotNull Entry createEntry(SelectionContext<Pack> context, int index);

    public @Nullable Entry getEntry(@Nullable Pack pack) {
        if (pack == null) return null;
        for (Entry entry : this.children()) {
            if (Objects.equals(entry.pack(), pack)) return entry;
        }
        return null;
    }

    protected void refreshEntries() {
        this.clearEntries();
        for (int i = 0; i < this.queried.size(); i++) {
            this.addEntry(this.createEntry(new SelectionContext<>(this.selection, this.queried.get(i)), i));
        }
        Entry focused = this.getFocused();
        if (focused != null && !this.queried.contains(focused.pack())) {
            this.setFocused(null);
        }
        this.clampScrollAmount();
    }

    public boolean isQueried() {
        return this.query.isQuerying();
    }

    protected void queryPacks() {
        this.queried.clear();

        if (PackedPacks.CONFIG.isDevMode()) {
            this.queried.addAll(this.packs);
        } else {
            CollectionsUtil.addIf(this.queried, this.packs, pack -> !this.options.isHidden(pack));
        }

        this.query.apply(this.queried);
        this.selection.retainAll(this.queried);
        this.refreshEntries();
    }

    public void scrollToTop() {
        this.setScrollAmount(0);
    }

    public void reload(Collection<Pack> packs) {
        this.packs.clear();

        for (Pack pack : packs) {
            if (pack != null && !this.packs.contains(pack)) {
                this.packs.add(pack);
            }
        }

        this.setFocused(null);
        this.clearSelection();
        this.queryPacks();
        this.scrollToTop();
    }

    public @NotNull List<Pack> copyPacks() {
        return List.copyOf(this.packs);
    }

    public @NotNull List<Pack> copySelection() {
        return List.copyOf(this.selection);
    }

    public @NotNull Query copyQuery() {
        return this.query.copy();
    }

    protected List<Pack> orderSelection(List<Pack> selection) {
        List<Pack> sortedSelection = new ArrayList<>(selection);
        sortedSelection.retainAll(this.queried);
        sortedSelection.sort(Comparator.comparingInt(this.queried::indexOf));
        return sortedSelection;
    }

    public List<Pack> getOrderedSelection() {
        return ImmutableList.copyOf(this.orderSelection(this.selection));
    }

    protected int[] getIndicesFromSelection(List<Pack> selection) {
        int[] selectionIndices = new int[selection.size()];
        for (int i = 0; i < selection.size(); i++) {
            int index = this.queried.indexOf(selection.get(i));
            selectionIndices[i] = index;
        }
        return selectionIndices;
    }

    protected int[] getSelectionIndices() {
        return this.getIndicesFromSelection(this.selection);
    }

    public void clearSelection() {
        this.selection.clear();
    }

    private void refresh() {
        this.clearSelection();
        this.queryPacks();
        this.scrollToTop();
    }

    public void sort(Query.SortOption sort) {
        if (this.query.setSort(sort)) {
            this.refresh();
        }
    }

    public void hideIncompatible(boolean hideIncompatible) {
        if (this.query.setHideIncompatible(hideIncompatible)) {
            this.clearSelection();
            this.queryPacks();
        }
    }

    public void search(@NotNull String search) {
        if (this.query.setSearch(search)) {
            this.refresh();
        }
    }

    private void addPack(Pack pack) {
        if (pack != null && !this.packs.contains(pack)) {
            int index = 0;
            for (Pack p : this.packs) {
                if (!this.options.isFixed(p) || this.options.getPosition(p) == Pack.Position.BOTTOM) break;
                index++;
            }
            this.packs.add(index, pack);
        }
    }

    public void add(Pack pack) {
        this.addPack(pack);
        this.queryPacks();
    }

    public void addAll(List<Pack> packs) {
        for (Pack pack : packs) {
            this.addPack(pack);
        }
        this.queryPacks();
    }

    public boolean move(Pack pack, int to) {
        if (this.options.isFixed(pack)) return false;

        int from = this.packs.indexOf(pack);
        if (from == -1 || to < 0 || to >= this.packs.size() || from == to) {
            return false;
        }

        this.packs.remove(from);
        this.packs.add(to, pack);
        this.queryPacks();
        this.setFocused(this.getEntry(pack));
        return true;
    }

    public boolean moveAll(List<Pack> selection, int to) {
        if (selection == null || selection.isEmpty() || to < 0 || to > this.packs.size()) {
            return false;
        }
        if (!new HashSet<>(this.packs).containsAll(selection)) {
            return false;
        }

        int index = to;
        for (Pack pack : selection) {
            int from = this.packs.indexOf(pack);
            if (from < to) index--;
        }

        this.packs.removeAll(selection);
        this.packs.addAll(index, selection);
        this.queryPacks();

        return true;
    }

    private void removePack(Pack pack) {
        if (this.packs.remove(pack) && this.selection.remove(pack)) {
            this.setFocused(null);
        }
    }

    public void remove(Pack pack) {
        this.removePack(pack);
        this.queryPacks();
    }

    public void removeAll(List<Pack> packs) {
        for (Pack pack : packs) {
            this.removePack(pack);
        }
        this.queryPacks();
    }

    public @Nullable Pack getLastSelected() {
        return !this.selection.isEmpty() ? this.selection.getLast() : null;
    }

    @Override
    public @Nullable Entry getSelected() {
        return this.getLastSelected() != null ? this.getEntry(this.getLastSelected()) : super.getSelected();
    }

    public boolean isSelected(Pack pack) {
        return this.selection.contains(pack);
    }

    public boolean isLocked() {
        return this.options.isLocked();
    }

    public void scrollToLastSelected() {
        Optional.ofNullable(this.getEntry(this.getLastSelected())).ifPresent(this::ensureVisible);
    }

    public void unselect(Pack pack) {
        this.selection.remove(pack);

        Entry entry = this.getEntry(pack);
        if (entry == this.getFocused()) {
            this.setFocused(null);
        }
        if (entry == this.getSelected()) {
            this.setSelected(null);
        }
    }

    public void select(Pack pack) {
        if (pack != null && this.queried.contains(pack)) {
            this.selection.remove(pack);
            this.selection.add(pack);
            Entry entry = this.getEntry(pack);
            this.setFocused(entry);
            this.setSelected(entry);
        }
    }

    public void selectAll(List<Pack> packs) {
        for (Pack pack : packs) {
            this.select(pack);
        }
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
        Pack selectionStart = this.getLastSelected();
        int lastSelectedIndex = this.queried.indexOf(selectionStart);
        int selectedPackIndex = this.queried.indexOf(pack);
        int[] selectionIndices = this.getSelectionIndices();
        Arrays.sort(selectionIndices);

        if (!(contains(selectionIndices, -1) || hasGap(selectionIndices, true)) && selectionIndices.length > 0) {
            if (selectionIndices[0] == lastSelectedIndex) {
                selectionStart = this.queried.get(selectionIndices[selectionIndices.length - 1]);
            } else if (selectionIndices[selectionIndices.length - 1] == lastSelectedIndex) {
                selectionStart = this.queried.get(selectionIndices[0]);
            }
        }

        int startIndex = this.queried.indexOf(selectionStart);
        if (selectedPackIndex != -1 && startIndex != -1) {
            this.clearSelection();
            for (int i = Math.min(selectedPackIndex, startIndex); i <= Math.max(selectedPackIndex, startIndex); i++) {
                Pack selected = this.queried.get(i);
                if (selected != pack) this.select(selected);
            }
        }

        this.select(pack);
    }

    public boolean isTransferable(Pack pack) {
        return testNullable(this.getEntry(pack), PackList.Entry::isTransferable);
    }

    public void transferAll() {
        List<Pack> payload = new ArrayList<>();

        for (int i = this.queried.size() - 1; i >= 0; i--) {
            Pack pack = this.queried.get(i);
            if (this.isTransferable(pack)) {
                payload.add(pack);
            }
        }

        if (!payload.isEmpty()) {
            this.sendEvent(new RequestTransferEvent(this, this.getLastSelected(), payload));
        }
    }

    protected void sendEvent(PackListEvent event) {
        this.listener.onEvent(event);
    }

    public abstract boolean canDrop(PackList source, List<Pack> payload, Pack trigger, double mouseX, double mouseY);

    protected abstract @Nullable List<Pack> handleDrop(PackList source, List<Pack> payload, Pack trigger, double mouseX, double mouseY);

    public abstract void renderDroppableZone(GuiGraphics guiGraphics, PackList source, List<Pack> payload, Pack trigger, int mouseX, int mouseY, float partialTick);

    public final void drop(PackList source, List<Pack> payload, Pack trigger, double mouseX, double mouseY) {
        List<Pack> dropped = this.handleDrop(source, payload, trigger, mouseX, mouseY);
        if (dropped != null && !dropped.isEmpty()) {
            if (source != this) {
                this.sendEvent(new DropEvent(source, this, dropped));
            } else {
                this.sendEvent(new MoveEvent(this, trigger, dropped));
            }
        }
    }

    protected void openFolder(FolderPack folderPack) {
        this.sendEvent(new FolderOpenEvent(this, folderPack));
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
            this.sendEvent(new SelectionEvent(this));
            this.ensureVisible(entry);
            return ComponentPath.path(entry, this);
        }
        this.setFocused(null);
        return null;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!this.isFocused()) {
            Pack lastSelected = this.getLastSelected();
            Entry entry = null;
            if (lastSelected != null) {
                entry = this.getEntry(lastSelected);
            } else if (!this.children().isEmpty()) {
                entry = this.getFirstElement();
            }
            if (entry != null) {
                this.select(entry.pack());
                this.ensureVisible(entry);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Entry entry = this.getEntry(this.getLastSelected());
        if (entry != null
            && entry.folderWidget != null
            && entry.pack() instanceof FolderPack folderPack
            && this.selection.size() == 1
            && isExpandFolder(keyCode, modifiers)) {
            this.openFolder(folderPack);
            return true;
        }
        if (isTransfer(keyCode, modifiers)) {
            if (entry != null && entry.transfer()) {
                playClickSound();
            }
            return entry != null;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isValidMouseClick(button)) {
            this.updateScrollingState(mouseX, mouseY, button);
        }
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        return ContainerEventHandlerPatch.super.mouseClickedAt(mouseX, mouseY, button) || ((AbstractSelectionListAccessor) this).packed_packs$scrolling();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // this.isHovered is evaluated right before renderWidget on AbstractWidget#render
        this.isHovered = this.isHovered && GuiUtil.isHovered(this, mouseX, mouseY);
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderListItems(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderListItems(guiGraphics, mouseX, mouseY, partialTick);

        Entry focused = this.getFocused();
        if (focused != null && focused.isFocused() && this.children().contains(focused)) {
            int outlineTop = focused.getY() - Entry.BACKGROUND_OFFSET;
            int outlineHeight = focused.getHeight() + Entry.BACKGROUND_OFFSET * 2;
            guiGraphics.renderOutline(focused.getX(), outlineTop, focused.getWidth(), outlineHeight, Theme.WHITE.getARGB());
        }
    }

    protected boolean beforeScrollbarX(double mouseX) {
        return !this.scrollbarVisible() || mouseX < this.getScrollbarPosition();
    }

    public @NotNull Snapshot captureState() {
        return new Snapshot(this, this.copyPacks(), this.copySelection(), this.copyQuery());
    }

    public void replaceState(@NotNull Snapshot snapshot) {
        Pack focused = mapOrNull(this.getFocused(), PackList.Entry::pack);
        this.packs.clear();

        for (Pack pack : snapshot.packs()) {
            if (pack != null && !this.packs.contains(pack)) {
                this.packs.add(pack);
            }
        }

        this.query.update(snapshot.query());
        this.queryPacks();

        this.clearSelection();
        for (Pack selected : snapshot.selection()) {
            this.select(selected);
        }
        this.setFocused(this.getEntry(focused));
    }

    public record Snapshot(
            PackList target,
            List<Pack> packs,
            List<Pack> selection,
            Query query
    ) implements Restorable.Snapshot<PackList.Snapshot> {
        public PackList.Snapshot validate(List<Pack> validPacks) {
            List<Pack> validated = new ArrayList<>(this.packs);
            validated.retainAll(validPacks);
            return new PackList.Snapshot(this.target, ImmutableList.copyOf(validated), this.selection, this.query);
        }
    }

    public abstract class Entry extends AbstractDynamicList<Entry>.Entry implements ContextMenuContainer {
        private static final Tooltip FOLDER_OPEN_INFO = Tooltip.create(FolderPack.FOLDER_OPEN_TEXT);
        protected static final int SPACING = 2;
        protected static final int BACKGROUND_OFFSET = 1;
        protected static final ColoredRect SELECTED_OVERLAY = new ColoredRect(Theme.BLUE_500.withAlpha(0.25F));
        protected final List<GuiEventListener> children = new ArrayList<>();
        protected final List<Renderable> renderables = new ArrayList<>();
        protected final List<Renderable> topRenderables = new ArrayList<>();
        protected final List<NarratableEntry> narratables = new ArrayList<>();
        private final MouseSelectionHandler<Pack> selectionHandler;
        private final SelectionContext<Pack> context;
        private final PackWidget packWidget;
        private final @Nullable PackListDevMenu devMenu;
        private FidgetzButton<FolderPack> folderWidget;
        private boolean stale = false;

        protected Entry(SelectionContext<Pack> context, int index) {
            super(index);
            this.context = context;
            this.selectionHandler = new MouseSelectionHandler<>(this, context);
            this.packWidget = this.addRenderableWidget(new PackWidget(
                    this.pack(),
                    PackList.this.assets,
                    this.getX(),
                    PackList.this.getRowTop(this.index),
                    this.getWidth(),
                    PackList.this.itemHeight,
                    SPACING
            ));
            boolean devMode = PackedPacks.CONFIG.isDevMode();
            if (this.pack() instanceof FolderPack folderPack && (devMode || Preferences.INSTANCE.folderPackWidget.get())) {
                this.folderWidget = this.addTopRenderableOnly(this.prependWidget(
                        Toggleable.applyPref(Preferences.INSTANCE.folderPackWidget, FidgetzButton.<FolderPack>builder())
                                .setTooltip(FOLDER_OPEN_INFO)
                                .setHeight(this.packWidget.getHeight() / 3)
                                .makeSquare()
                                .setSprite(GuiConstants.HAMBURGER_SPRITE)
                                .setMetadata(folderPack)
                                .setOnPress(this::openFolder)
                                .build()
                ));
            }
            this.devMenu = devMode ? new PackListDevMenu(PackList.this.options, this.context, this::onRequire) : null;
            ModAdditions.addToEntry(PackList.this.options.getConfig().packType(), this);
        }

        public Pack pack() {
            return this.context.item();
        }

        public <U extends GuiEventListener & Renderable> U addRenderableWidget(U widget) {
            this.children.add(widget);
            this.renderables.add(widget);
            if (widget instanceof NarratableEntry narratable) this.narratables.add(narratable);
            return widget;
        }

        public <U extends GuiEventListener> U prependWidget(U widget) {
            this.children.addFirst(widget);
            if (widget instanceof NarratableEntry narratable) this.narratables.add(narratable);
            return widget;
        }

        public <U extends Renderable> U addTopRenderableOnly(U renderable) {
            this.topRenderables.add(renderable);
            return renderable;
        }

        public boolean isTransferable() {
            return !PackList.this.isLocked();
        }

        public boolean isSelected() {
            return this.context.isSelected();
        }

        public boolean isSelectedLast() {
            return this.context.isSelectedLast();
        }

        protected void sendPacks(Pack trigger, List<Pack> payload) {
            PackList.this.sendEvent(new RequestTransferEvent(PackList.this, trigger, payload));
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
                PackList.this.sendEvent(new RequestTransferEvent(PackList.this, this.pack()));
                return true;
            }

            return this.sendSelection();
        }

        protected void onRequire(Pack trigger, List<Pack> requiredPacks) {
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
                        PackList.this.sendEvent(new RequestTransferEvent(PackList.this, this.pack()));
                        return false;
                    }
                }
                case DRAG -> PackList.this.sendEvent(new DragEvent(
                        PackList.this,
                        PackList.this.getOrderedSelection().reversed(),
                        this.pack(),
                        this.packWidget.getSprite()
                ));
            }

            if (action.shouldSelect()) {
                PackList.this.sendEvent(new SelectionEvent(PackList.this));
            }

            return true;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return PackList.this.isHovered() && PackList.this.beforeScrollbarX(mouseX) && super.isMouseOver(mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return false;
            }
            return this.handleMouseAction(this.selectionHandler.mouseClicked(mouseX, mouseY, button));
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return this.handleMouseAction(this.selectionHandler.mouseReleased(mouseX, mouseY, button));
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return this.handleMouseAction(this.selectionHandler.mouseDragged(mouseX, mouseY, button, dragX, dragY));
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            boolean keyPressed = super.keyPressed(keyCode, scanCode, modifiers);
            if (!keyPressed) {
                if (isOpenFile(keyCode, modifiers)) {
                    PackUtil.openPack(this.pack());
                    return true;
                }
                if (isOpenFolder(keyCode, modifiers)) {
                    PackUtil.openParent(this.pack());
                    return true;
                }
                if (this.canOperateFile()) {
                    if (isDelete(keyCode, modifiers)) {
                        this.deletePack();
                        return true;
                    } else if (isRename(keyCode, modifiers)) {
                        this.renamePack();
                        return true;
                    }
                }
            }
            return keyPressed;
        }

        @Override
        public void renderBack(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            if (!this.pack().getCompatibility().isCompatible() && !PackList.this.options.getConfig().isIncompatibleWarningsHidden()) {
                int backgroundLeft = this.getX() + BACKGROUND_OFFSET;
                int backgroundRight = backgroundLeft + this.getWidth() - BACKGROUND_OFFSET * 2;
                guiGraphics.fill(backgroundLeft, this.getY(), backgroundRight, this.getBottom(), Theme.RED_900.getARGB());
            }
        }

        protected abstract void renderForeground(GuiGraphics guiGraphics, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick);

        private void renderSelection(GuiGraphics guiGraphics, int top, int left, int width, int height) {
            if (this.isSelected()) {
                int overlayLeft = this.getX() + BACKGROUND_OFFSET;
                int overlayWidth = this.getWidth() - BACKGROUND_OFFSET * 2;
                pick(isSelectedLast(), GuiConstants.WHITE_OVERLAY, SELECTED_OVERLAY)
                        .render(guiGraphics, overlayLeft, this.getY(), overlayWidth, this.getHeight());
            }
            if (this.isSelected() || this.isFocused()) {
                int outlineTop = this.getY() - BACKGROUND_OFFSET;
                int outlineHeight = this.getHeight() + BACKGROUND_OFFSET * 2;
                if (!this.isFocused()) {
                    guiGraphics.renderOutline(left, outlineTop, width, outlineHeight, Theme.BLUE_500.getARGB());
                }
            }
        }

        protected void renderTop(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (this.folderWidget != null) {
                int folderWidgetY = this.getBottom() - this.folderWidget.getHeight() - BACKGROUND_OFFSET;
                this.folderWidget.setPosition(this.packWidget.getContentLeft(), folderWidgetY);
            }

            for (Renderable renderable : this.topRenderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        private void renderWidget(GuiGraphics guiGraphics, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            this.packWidget.setPosition(left, top);
            this.packWidget.setWidth(width);

            for (Renderable renderable : this.renderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            this.renderSelection(guiGraphics, top, left, width, height);
            this.renderForeground(guiGraphics, top, left, width, height, mouseX, mouseY, hovering, partialTick);
            this.renderTop(guiGraphics, mouseX, mouseY, partialTick);

            if (this.devMenu != null) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 1f);
                this.devMenu.renderDevSprites(guiGraphics, top, left, width);
                guiGraphics.pose().popPose();
            }
        }

        @Override
        public final void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            this.renderWidget(guiGraphics, top, left, width, height, mouseX, mouseY, hovering && PackList.this.isHovered() && PackList.this.beforeScrollbarX(mouseX), partialTick);
        }

        @Override
        public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
            PackList.this.setFocused(this);
            ContextMenuContainer.super.buildItems(builder
                            .add(new PackMenuHeader(this.pack(), this.packWidget.getSprite()))
                            .whenNonNull(this.devMenu)
                            .ifTrue(PackListDevMenu::onBuildHeader)
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
                            ),
                    mouseX,
                    mouseY
            );
        }

        private void openFolder() {
            PackList.this.openFolder(this.folderWidget.getMetadata());
        }

        public boolean canOperateFile() {
            return PackList.this.fileOps.isOperable(this.pack());
        }

        public void deletePack() {
            if (PackList.this.fileOps.deletePack(this.pack())) {
                this.stale = true;
                PackList.this.remove(this.pack());
                PackList.this.sendEvent(new FileDeleteEvent(PackList.this));
            } else {
                ToastUtil.onFileFailToast(ToastUtil.getDeleteFailText(this.pack().getTitle().getString()));
            }
        }

        public void renamePack() {
            PackList.this.sendEvent(new FileRenameOpenEvent(PackList.this, this.pack()));
        }

        public void onRename(Component newName) {
            this.stale = true;
            this.packWidget.onRename(newName);
            if (this.folderWidget != null) {
                this.folderWidget.active = false;
            }
        }

        public boolean isStale() {
            return this.stale;
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {
            return this.narratables;
        }

        @Override
        public int getY() {
            return super.getY() - BACKGROUND_OFFSET;
        }

        @Override
        public int getHeight() {
            return super.getHeight() + BACKGROUND_OFFSET * 2;
        }
    }
}
