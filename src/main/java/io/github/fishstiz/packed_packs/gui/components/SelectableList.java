package io.github.fishstiz.packed_packs.gui.components;

import com.google.common.primitives.Ints;
import io.github.fishstiz.packed_packs.util.lang.IntsUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;
import java.util.function.Predicate;

public class SelectableList<T> {
    protected final List<T> items = new ObjectArrayList<>();
    protected final List<T> selectedItems = new ObjectArrayList<>();
    protected final List<T> visibleItems = new ObjectArrayList<>();
    private final Predicate<T> filter;

    public SelectableList(Predicate<T> filter) {
        this.filter = filter;
    }

    public SelectableList() {
        this(Objects::nonNull);
    }

    public List<T> getItems() {
        return Collections.unmodifiableList(this.items);
    }

    public List<T> getSelection() {
        return Collections.unmodifiableList(this.selectedItems);
    }

    public List<T> getVisibleItems() {
        return Collections.unmodifiableList(this.visibleItems);
    }

    public int size() {
        return this.items.size();
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public int indexOf(T item) {
        return this.items.indexOf(item);
    }

    protected boolean filter(T item) {
        return this.filter.test(item);
    }

    public void refresh() {
        this.visibleItems.clear();
        for (T item : this.items) {
            if (this.filter(item)) {
                this.visibleItems.add(item);
            }
        }
        this.selectedItems.retainAll(this.visibleItems);
    }

    public void add(T item) {
        this.items.add(item);
    }

    public void replaceAll(Collection<T> items) {
        this.items.clear();
        Set<T> seen = new ObjectOpenHashSet<>(items.size());
        for (T item : items) {
            if (item != null && seen.add(item)) {
                this.items.add(item);
            }
        }
    }

    public boolean remove(T item) {
        boolean removed = this.items.remove(item);
        this.selectedItems.remove(item);
        this.visibleItems.remove(item);
        return removed;
    }

    public void insertOrMove(int index, T item) {
        int previous = this.items.indexOf(item);
        if (previous != -1 && previous < index) {
            index--;
        }

        this.items.remove(item);
        this.items.add(Math.clamp(index, 0, this.items.size()), item);
    }

    public boolean move(int index, T item) {
        int from = this.items.indexOf(item);
        if (from == -1 || index < 0 || index >= this.items.size() || from == index) {
            return false;
        }

        this.items.remove(from);
        this.items.add(index, item);
        return true;
    }

    public boolean moveAll(int index, List<T> selection) {
        if (selection == null || selection.isEmpty() || index < 0 || index > this.items.size()) {
            return false;
        }
        if (!new ObjectOpenHashSet<>(this.items).containsAll(selection)) {
            return false;
        }

        selection = this.orderByVisible(selection);

        int to = index;
        for (T item : selection) {
            int from = this.items.indexOf(item);
            if (from < index) to--;
        }

        this.items.removeAll(selection);
        this.items.addAll(to, selection);
        return true;
    }

    public T getLastSelected() {
        return !this.selectedItems.isEmpty() ? this.selectedItems.getLast() : null;
    }

    public void clearSelection() {
        this.selectedItems.clear();
    }

    public boolean isSelected(T item) {
        return this.selectedItems.contains(item);
    }

    public void unselect(T item) {
        this.selectedItems.remove(item);
    }

    public boolean select(T item) {
        if (item != null && this.visibleItems.contains(item)) {
            this.selectedItems.remove(item);
            this.selectedItems.add(item);
            return true;
        }
        return false;
    }

    public void selectRange(T targetItem) {
        T anchor = this.getLastSelected();
        int anchorIndex = this.visibleItems.indexOf(anchor);
        int targetIndex = this.visibleItems.indexOf(targetItem);
        int[] indices = this.getSelectionVisibleIndices();
        Arrays.sort(indices);

        if (!(Ints.contains(indices, -1) || IntsUtil.hasGap(indices, true)) && indices.length > 0) {
            if (indices[0] == anchorIndex) {
                anchor = this.visibleItems.get(indices[indices.length - 1]);
            } else if (indices[indices.length - 1] == anchorIndex) {
                anchor = this.visibleItems.get(indices[0]);
            }
        }

        int start = this.visibleItems.indexOf(anchor);
        if (targetIndex != -1 && start != -1) {
            this.clearSelection();
            for (int i = Math.min(targetIndex, start); i <= Math.max(targetIndex, start); i++) {
                T selected = this.visibleItems.get(i);
                if (selected != targetItem) this.select(selected);
            }
        }

        this.select(targetItem);
    }

    public List<T> getOrderedSelection() {
        return this.orderByVisible(this.selectedItems);
    }

    protected List<T> orderByVisible(List<T> selection) {
        List<T> orderedSelection = new ObjectArrayList<>(selection);
        orderedSelection.retainAll(this.visibleItems);
        orderedSelection.sort(Comparator.comparingInt(this.visibleItems::indexOf));
        return orderedSelection;
    }

    public boolean isValidInsertPosition(int index, List<T> selection) {
        int[] indices = this.getVisibilityIndices(selection);
        if (indices.length == 0) return false;
        if (IntsUtil.hasGap(indices)) return true;
        Arrays.sort(indices);

        int lastSelectionIndex = indices[indices.length - 1];
        if (!this.items.isEmpty()) {
            int lastItemIndex = this.items.indexOf(this.items.getLast());
            if (index == -1 && lastItemIndex == lastSelectionIndex) {
                return false;
            }
        }

        return index != indices[0] && index - 1 != lastSelectionIndex;
    }

    private int[] getVisibilityIndices(List<T> selection) {
        int[] selectionIndices = new int[selection.size()];
        for (int i = 0; i < selection.size(); i++) {
            int index = this.visibleItems.indexOf(selection.get(i));
            selectionIndices[i] = index;
        }
        return selectionIndices;
    }

    private int[] getSelectionVisibleIndices() {
        return this.getVisibilityIndices(this.selectedItems);
    }
}
