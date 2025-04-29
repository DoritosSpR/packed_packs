package io.github.fishstiz.packed_packs.gui.components.list;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.fishstiz.fidgetz.gui.Background;
import io.github.fishstiz.fidgetz.gui.Metadata;
import io.github.fishstiz.fidgetz.gui.components.AbstractDynamicList;
import io.github.fishstiz.packed_packs.gui.components.PackListContainer;
import io.github.fishstiz.packed_packs.gui.metadata.Flex;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.gui.event.*;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

import static com.google.common.primitives.Ints.contains;
import static io.github.fishstiz.fidgetz.util.WidgetUtil.playClickSound;
import static io.github.fishstiz.packed_packs.util.InputUtil.*;
import static io.github.fishstiz.packed_packs.util.lang.IntsUtil.hasGap;
import static io.github.fishstiz.packed_packs.util.lang.ObjectsUtil.*;

public abstract class PackListBase<T extends PackListBase<T>.Entry> extends AbstractDynamicList<T> implements PackList, Metadata<Flex> {
    protected static final int OFFSET_Y = 2;
    protected static final int ITEM_HEIGHT = 32;
    protected static final int ROW_GAP = 3;
    protected final List<Pack> packs = new ArrayList<>();
    private final List<Pack> queried = new ArrayList<>();
    private final List<Pack> selection = new ArrayList<>();
    private final PackListContainer parent;
    private final Query query = new Query();
    private Flex metadata;

    protected PackListBase(PackListContainer parent) {
        super(ITEM_HEIGHT, DEFAULT_SCROLLBAR_OFFSET, OFFSET_Y, ROW_GAP);

        this.parent = parent;
        this.queryPacks();
    }

    protected abstract @NotNull T createEntry(Pack pack, int index);

    public @Nullable T getEntry(@Nullable Pack pack) {
        if (pack == null) return null;
        for (T entry : this.children()) {
            if (entry.pack == pack) return entry;
        }
        return null;
    }

    protected void refreshEntries() {
        this.clearEntries();
        for (int i = 0; i < this.queried.size(); i++) {
            this.addEntry(this.createEntry(this.queried.get(i), i));
        }
        T focused = this.getFocused();
        if (focused != null && !this.queried.contains(focused.pack)) {
            this.setFocused(null);
        }
        this.clampScrollAmount();
    }

    public boolean isQueried() {
        return this.query.isQuerying();
    }

    protected void queryPacks() {
        this.queried.clear();
        this.queried.addAll(this.packs);
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

        this.queryPacks();
        this.scrollToTop();
    }

    @Override
    public @NotNull @Unmodifiable List<Pack> getPacksCopy() {
        return List.copyOf(this.packs);
    }

    @Override
    public @NotNull @Unmodifiable List<Pack> getSelectionCopy() {
        return List.copyOf(this.selection);
    }

    @Override
    public @NotNull Query getQueryCopy() {
        return this.query.copy();
    }

    protected List<Pack> orderSelection(List<Pack> selection) {
        List<Pack> sortedSelection = new ArrayList<>(selection);
        sortedSelection.retainAll(this.queried);
        sortedSelection.sort(Comparator.comparingInt(this.queried::indexOf));
        return sortedSelection;
    }

    public @NotNull @Unmodifiable List<Pack> getOrderedSelection() {
        return List.copyOf(this.orderSelection(this.selection));
    }

    protected int[] getSelectionIndices() {
        int[] selectionIndices = new int[this.selection.size()];
        for (int i = 0; i < this.selection.size(); i++) {
            int index = this.queried.indexOf(this.selection.get(i));
            selectionIndices[i] = index;
        }
        return selectionIndices;
    }

    @Override
    public void clearSelection() {
        this.selection.clear();
    }

    private void refresh() {
        this.clearSelection();
        this.queryPacks();
        this.scrollToTop();
    }

    public void query(boolean incompatibleHidden, Query.SortOption sort, String search) {
        if (this.query.update(incompatibleHidden, sort, search)) {
            this.refresh();
        }
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

    @Override
    public void add(Pack pack) {
        if (pack != null && !this.packs.contains(pack)) {
            this.packs.addFirst(pack);
            this.queryPacks();
        }
    }

    @Override
    public void insert(Pack pack, int index) {
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
    public boolean move(Pack pack, int to) {
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

    @Override
    public boolean move(List<Pack> selection, int to) {
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

    @Override
    public void remove(Pack pack) {
        if (this.packs.remove(pack)) {
            this.selection.remove(pack);
            this.queryPacks();
            this.setFocused(null);
        }
    }

    public @Nullable Pack getLastSelected() {
        return !this.selection.isEmpty() ? this.selection.getLast() : null;
    }

    @Override
    public @Nullable T getSelected() {
        return this.getLastSelected() != null ? this.getEntry(this.getLastSelected()) : super.getSelected();
    }

    public boolean isSelected(Pack pack) {
        return this.selection.contains(pack);
    }

    public void scrollToLastSelected() {
        Optional.ofNullable(this.getEntry(this.getLastSelected())).ifPresent(this::ensureVisible);
    }

    @Override
    public void unselect(Pack pack) {
        this.selection.remove(pack);

        T entry = this.getEntry(pack);
        if (entry == this.getFocused()) {
            this.setFocused(null);
        }
        if (entry == this.getSelected()) {
            this.setSelected(null);
        }
    }

    @Override
    public void select(Pack pack) {
        if (pack != null && this.queried.contains(pack)) {
            this.selection.remove(pack);
            this.selection.add(pack);
            T entry = this.getEntry(pack);
            this.setFocused(entry);
            this.setSelected(entry);
        }
    }

    @Override
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

    @Override
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

    protected void sendEvent(Event event) {
        this.parent.onEvent(event);
    }

    protected abstract @Nullable List<Pack> onDrop(PackList source, List<Pack> selection, double mouseX, double mouseY);

    @Override
    public final void drop(PackList source, List<Pack> selection, double mouseX, double mouseY) {
        List<Pack> dropped = this.onDrop(source, selection, mouseX, mouseY);
        if (dropped != null && !dropped.isEmpty()) {
            if (source != this) {
                this.sendEvent(new TransferEvent(source, this, List.copyOf(dropped)));
            } else {
                this.sendEvent(new MoveEvent(this, List.copyOf(dropped)));
            }
        }
    }

    protected abstract void renderDroppableZone(GuiGraphics guiGraphics, PackList source, List<Pack> selection, int mouseX, int mouseY, float partialTick);

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        if (this.parent.isDraggingSelection()) {
            DragEvent event = this.parent.getDragged();

            if (event != null) {
                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                poseStack.translate(0, 0, this.parent.getDroppableZ());
                this.renderDroppableZone(guiGraphics, event.target(), event.dragged(), mouseX, mouseY, partialTick);
                poseStack.popPose();
            }
        }
    }

    private @Nullable ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation arrowNavigation) {
        T entry = switch (arrowNavigation.direction()) {
            case UP -> this.getPreviousEntry();
            case DOWN -> this.getNextEntry();
            default -> null;
        };
        if (entry != null) {
            if (isRangeModifierActive()) {
                this.selectRange(entry.pack);
            } else {
                this.selectExclusive(entry.pack);
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
            T entry = null;
            if (lastSelected != null) {
                entry = this.getEntry(lastSelected);
            } else if (!this.children().isEmpty()) {
                entry = this.getFirstElement();
            }
            if (entry != null) {
                this.select(entry.pack);
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
        if (this.parent.isDraggingSelection()) {
            return true;
        }
        if (isTransfer(keyCode, modifiers)) {
            Entry entry = this.getEntry(this.getLastSelected());
            if (entry != null && entry.transfer()) {
                playClickSound();
            }
            return entry != null;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void replaceState(Snapshot snapshot) {
        this.packs.clear();

        for (Pack pack : snapshot.packs) {
            if (pack != null && !this.packs.contains(pack)) {
                this.packs.add(pack);
            }
        }

        this.query.update(snapshot.query);
        this.queryPacks();

        this.clearSelection();
        for (Pack selected : snapshot.selection) {
            this.select(selected);
        }
        this.scrollToLastSelected();
    }

    @Override
    public Flex getMetadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(Flex metadata) {
        this.metadata = metadata;
    }

    public abstract class Entry extends AbstractDynamicList<T>.Entry implements PackList.Entry {
        protected static final int SPACING = 2;
        protected static final int BACKGROUND_OFFSET = 1;
        protected static final Background.Color OVERLAY = new Background.Color(Theme.WHITE.withAlpha(0.25F));
        protected static final Background.Color SELECTED_OVERLAY = new Background.Color(Theme.BLUE_500.withAlpha(0.25F));
        protected final List<GuiEventListener> children = new ArrayList<>();
        protected final List<NarratableEntry> narratables = new ArrayList<>();
        protected final Pack pack;
        private final PackWidget packWidget;

        protected Entry(Pack pack, int index) {
            super(index);

            this.pack = pack;
            this.packWidget = new PackWidget(
                    this.pack,
                    this.getX(),
                    PackListBase.this.getRowTop(this.index),
                    this.getWidth(),
                    PackListBase.this.itemHeight,
                    SPACING
            );
            this.children.add(this.packWidget);
            this.narratables.add(this.packWidget);
        }

        @Override
        public Pack getPack() {
            return this.pack;
        }

        public boolean isSelected() {
            return PackListBase.this.selection.contains(this.pack);
        }

        public boolean isSelectedLast() {
            return PackListBase.this.getLastSelected() == this.pack;
        }

        private boolean transferPack(Pack selected, PackList target) {
            if (PackListBase.this.isTransferable(selected)) {
                PackListBase.this.remove(selected);
                target.add(selected);
                return true;
            }
            return false;
        }

        private boolean transferSelection(List<Pack> selection, PackList target) {
            if (selection.isEmpty()) return false;

            Pack lastSelected = PackListBase.this.getLastSelected();
            PackListBase.this.clearSelection();
            target.clearSelection();

            List<Pack> transferred = new ArrayList<>();
            for (Pack selected : selection) {
                if (this.transferPack(selected, target)) {
                    transferred.add(selected);
                    target.select(selected);
                }
            }
            PackListBase.this.select(lastSelected);
            target.select(lastSelected);

            if (!transferred.isEmpty()) {
                PackListBase.this.sendEvent(new TransferEvent(PackListBase.this, target, List.copyOf(transferred)));
                return true;
            }

            return false;
        }

        public boolean transfer() {
            PackList target = PackListBase.this.parent.getTarget(PackListBase.this);

            if (!this.isSelected() && this.transferPack(this.pack, target)) {
                target.selectExclusive(this.pack);
                PackListBase.this.sendEvent(new TransferEvent(PackListBase.this, target, List.of(this.pack)));
                return true;
            }

            return this.transferSelection(PackListBase.this.getOrderedSelection().reversed(), target);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isLeftClick(button) && mouseX <= this.getRight()) {
                if (isRangeModifierActive()) {
                    PackListBase.this.selectRange(this.pack);
                    PackListBase.this.sendEvent(new SelectionEvent(PackListBase.this));
                } else if (isSelectModifierActive()) {
                    PackListBase.this.selectToggle(this.pack);
                    PackListBase.this.sendEvent(new SelectionEvent(PackListBase.this));
                } else if (!this.isSelected()) {
                    PackListBase.this.selectExclusive(this.pack);
                    PackListBase.this.sendEvent(new SelectionEvent(PackListBase.this));
                } else if (this.isSelected() && !this.isSelectedLast()) {
                    PackListBase.this.select(this.pack);
                    PackListBase.this.sendEvent(new SelectionEvent(PackListBase.this));
                }
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (isLeftClick(button)
                && !isRangeModifierActive()
                && !isSelectModifierActive()
                && !PackListBase.this.parent.isDraggingSelection()
                && PackListBase.this.selection.size() > 1
                && this.isSelected()
                && this.isMouseOver(mouseX, mouseY)) {
                PackListBase.this.selectExclusive(this.pack);
                PackListBase.this.sendEvent(new SelectionEvent(PackListBase.this));
                return true;
            }

            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (this.isSelected()
                && isLeftClick(button)
                && !isRangeModifierActive()
                && !isSelectModifierActive()
                && !PackListBase.this.parent.isDraggingSelection()
                && DragEvent.exceedsThreshold(dragX, dragY)
                && mouseX <= this.getRight()
                && this.isMouseOver(mouseX, mouseY)) {
                PackListBase.this.sendEvent(new DragEvent(PackListBase.this, PackListBase.this.parent));
                return true;
            }

            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public void renderBack(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            if (!this.pack.getCompatibility().isCompatible()) {
                int backgroundTop = this.getY() - BACKGROUND_OFFSET;
                int backgroundLeft = this.getX() + BACKGROUND_OFFSET;
                int backgroundBottom = backgroundTop + this.getHeight() + BACKGROUND_OFFSET * 2;
                int backgroundRight = backgroundLeft + this.getWidth() - BACKGROUND_OFFSET * 2;
                guiGraphics.fill(backgroundLeft, backgroundTop, backgroundRight, backgroundBottom, Theme.RED_900.getARGB());
            }
        }

        protected abstract void renderForeground(GuiGraphics guiGraphics, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick);

        private void renderSelection(GuiGraphics guiGraphics, int top, int left, int width, int height) {
            if (this.isSelected()) {
                int overlayTop = this.getY() - BACKGROUND_OFFSET;
                int overlayLeft = this.getX() + BACKGROUND_OFFSET;
                int overlayWidth = this.getWidth() - BACKGROUND_OFFSET * 2;
                int overlayHeight = this.getHeight() + BACKGROUND_OFFSET * 2;
                pick(isSelectedLast(), OVERLAY, SELECTED_OVERLAY).render(guiGraphics, overlayLeft, overlayTop, overlayWidth, overlayHeight);
            }
            if (this.isSelected() || this.isFocused()) {
                int outlineTop = top - BACKGROUND_OFFSET * 2;
                int outlineHeight = height + BACKGROUND_OFFSET * 4;

                if (this.isFocused()) {
                    PoseStack poseStack = guiGraphics.pose();
                    poseStack.pushPose();
                    poseStack.translate(0, 0, 1f);
                    guiGraphics.renderOutline(left, outlineTop, width, outlineHeight, Theme.WHITE.getARGB());
                    poseStack.popPose();
                } else {
                    guiGraphics.renderOutline(left, outlineTop, width, outlineHeight, Theme.BLUE_500.getARGB());
                }
            }
        }

        private void renderWidget(GuiGraphics guiGraphics, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            if (this.packWidget.getIcon() == null) { // lazy loads icon as render is not called if entry is not visible
                this.packWidget.setIcon(PackListBase.this.parent.getIcon(this.pack));
            }

            this.packWidget.setPosition(left, top);
            this.packWidget.setWidth(width);
            this.packWidget.render(guiGraphics, mouseX, mouseY, partialTick);

            this.renderSelection(guiGraphics, top, left, width, height);
            this.renderForeground(guiGraphics, top, left, width, height, mouseX, mouseY, hovering, partialTick);
        }

        @Override
        public final void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            this.renderWidget(guiGraphics, top, left, width, height, mouseX, mouseY, hovering, partialTick);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {
            return this.narratables;
        }
    }
}
