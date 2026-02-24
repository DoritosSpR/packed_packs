package io.github.fishstiz.packed_packs.gui.components.pack;

import com.google.common.primitives.Ints;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.history.Restorable;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.packed_packs.util.lang.IntsUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.ToIntFunction;

public class PackListModel implements Restorable<PackListModel.Snapshot> {
    private final List<Pack> packs = new ObjectArrayList<>();
    private final List<Pack> visiblePacks = new ObjectArrayList<>();
    private final List<Pack> selectedPacks = new ObjectArrayList<>();
    private final PackOptionsContext options;
    private Query query = new Query();

    public PackListModel(PackOptionsContext options) {
        this.options = options;
    }

    public List<Pack> getPacks() {
        return this.packs;
    }

    public List<Pack> getVisiblePacks() {
        return this.visiblePacks;
    }

    public List<Pack> getSelection() {
        return this.selectedPacks;
    }

    public List<Pack> getOrderedSelection() {
        return this.orderByVisible(this.selectedPacks);
    }

    public Pack getLastSelected() {
        return !this.selectedPacks.isEmpty() ? this.selectedPacks.getLast() : null;
    }

    public int size() {
        return this.packs.size();
    }

    public boolean isEmpty() {
        return this.packs.isEmpty();
    }

    public int indexOf(Pack pack) {
        return this.packs.indexOf(pack);
    }

    private boolean updateQuery(Query query) {
        Query previousQuery = this.query;
        this.query = Objects.requireNonNull(query, "query");
        return !Objects.equals(this.query, previousQuery);
    }

    public boolean sort(Query.SortOption sort) {
        return this.updateQuery(this.query.withSort(sort));
    }

    public boolean search(String search) {
        return this.updateQuery(this.query.withSearch(search));
    }

    public boolean hideIncompatible(boolean hide) {
        return this.updateQuery(this.query.withHideIncompatible(hide));
    }

    public boolean isQueried() {
        return this.query.hasQuery();
    }

    private boolean filter(Pack pack) {
        if (!Config.get().isDevMode() && this.options.isHidden(pack)) {
            return false;
        }
        return this.query.test(pack);
    }

    public void refresh() {
        this.visiblePacks.clear();
        for (Pack pack : this.packs) {
            if (this.filter(pack)) {
                this.visiblePacks.add(pack);
            }
        }

        this.selectedPacks.retainAll(this.visiblePacks);

        if (this.query.sort() != null) {
            this.visiblePacks.sort(this.query);
        }
    }

    private List<Pack> orderByVisible(List<Pack> selection) {
        List<Pack> orderedSelection = new ObjectArrayList<>(selection);
        orderedSelection.retainAll(this.visiblePacks);
        orderedSelection.sort(Comparator.comparingInt(this.visiblePacks::indexOf));
        return orderedSelection;
    }

    public void clearSelection() {
        this.selectedPacks.clear();
    }

    public boolean isSelected(Pack pack) {
        return this.selectedPacks.contains(pack);
    }

    public void unselect(Pack item) {
        this.selectedPacks.remove(item);
    }

    public boolean select(Pack item) {
        if (item != null && this.visiblePacks.contains(item)) {
            this.selectedPacks.remove(item);
            this.selectedPacks.add(item);
            return true;
        }
        return false;
    }

    public void selectRange(Pack targetPack) {
        Pack anchor = this.getLastSelected();
        int anchorIndex = this.visiblePacks.indexOf(anchor);
        int targetIndex = this.visiblePacks.indexOf(targetPack);
        int[] indices = this.getVisibilityIndices(this.selectedPacks);
        Arrays.sort(indices);

        if (!(Ints.contains(indices, -1) || IntsUtil.hasGap(indices, true)) && indices.length > 0) {
            if (indices[0] == anchorIndex) {
                anchor = this.visiblePacks.get(indices[indices.length - 1]);
            } else if (indices[indices.length - 1] == anchorIndex) {
                anchor = this.visiblePacks.get(indices[0]);
            }
        }

        int start = this.visiblePacks.indexOf(anchor);
        if (targetIndex != -1 && start != -1) {
            this.clearSelection();
            for (int i = Math.min(targetIndex, start); i <= Math.max(targetIndex, start); i++) {
                Pack selected = this.visiblePacks.get(i);
                if (selected != targetPack) this.select(selected);
            }
        }

        this.select(targetPack);
    }

    public void add(Pack pack) {
        if (pack != null && !this.packs.contains(pack)) {
            int index = 0;
            for (Pack p : this.packs) {
                if (!this.options.isFixed(p) || this.options.getPosition(p) == Pack.Position.BOTTOM) break;
                index++;
            }
            this.packs.add(index, pack);
        }
    }

    public void replaceAll(Collection<Pack> packs) {
        this.packs.clear();
        Set<Pack> seen = new ObjectOpenHashSet<>(packs.size());
        for (Pack pack : packs) {
            if (pack != null && seen.add(pack)) {
                this.packs.add(pack);
            }
        }
    }

    public boolean remove(Pack pack) {
        boolean removed = this.packs.remove(pack);
        this.selectedPacks.remove(pack);
        this.visiblePacks.remove(pack);
        return removed;
    }


    public void insertOrMove(int index, Pack pack) {
        int previous = this.packs.indexOf(pack);
        if (previous != -1 && previous < index) {
            index--;
        }

        this.packs.remove(pack);
        this.packs.add(Math.clamp(index, 0, this.packs.size()), pack);
    }

    public boolean move(int index, Pack pack) {
        if (this.options.isFixed(pack)) {
            return false;
        }

        int from = this.packs.indexOf(pack);
        if (from == -1 || index < 0 || index >= this.packs.size() || from == index) {
            return false;
        }

        this.packs.remove(from);
        this.packs.add(index, pack);
        return true;
    }

    public boolean moveAll(int index, List<Pack> selection) {
        if (selection == null || selection.isEmpty() || index < 0 || index > this.packs.size()) {
            return false;
        }
        if (!new ObjectOpenHashSet<>(this.packs).containsAll(selection)) {
            return false;
        }

        selection = this.orderByVisible(selection);

        int to = index;
        for (Pack pack : selection) {
            int from = this.packs.indexOf(pack);
            if (from < index) to--;
        }

        this.packs.removeAll(selection);
        this.packs.addAll(to, selection);
        return true;
    }

    public boolean moveUp(Pack pack) {
        return this.movePack(this::getMoveUpIndex, pack);
    }

    public boolean moveDown(Pack pack) {
        return this.movePack(this::getMoveDownIndex, pack);
    }

    public List<Pack> moveSelectionUp(List<Pack> selection) {
        return this.moveSelection(this::getMoveUpIndex, this.orderByVisible(selection));
    }

    public List<Pack> moveSelectionDown(List<Pack> selection) {
        return this.moveSelection(this::getMoveDownIndex, this.orderByVisible(selection).reversed());
    }

    private boolean movePack(ToIntFunction<Pack> moveIndexFn, Pack pack) {
        if (this.packs.contains(pack)) {
            int targetIndex = moveIndexFn.applyAsInt(pack);
            return targetIndex > -1 && this.move(targetIndex, pack);
        }
        return false;
    }

    private List<Pack> moveSelection(ToIntFunction<Pack> moveIndexFn, List<Pack> selection) {
        List<Pack> moved = new ObjectArrayList<>();
        Set<Pack> packSet = new ObjectOpenHashSet<>(this.packs);
        for (int i = 0; i < selection.size(); i++) {
            Pack pack = selection.get(i);
            if (packSet.contains(pack)) {
                int index = moveIndexFn.applyAsInt(pack);
                if (index > -1 && this.move(index, pack)) {
                    moved.add(pack);
                } else if (i == 0) {
                    return Collections.emptyList();
                }
            }
        }
        return moved;
    }

    public boolean canMoveUp(Pack pack) {
        if (!this.canMove(pack)) return false;

        if (this.isSelected(pack)) {
            List<Pack> selection = this.getOrderedSelection();
            if (selection.size() > 1) {
                int index = this.packs.indexOf(selection.getFirst());
                int moveIndex = index > -1 ? this.getMoveUpIndex(pack) : -1;
                return index > 0 && moveIndex > -1 && !this.options.isFixed(this.packs.get(moveIndex));
            }
        }

        int index = this.packs.indexOf(pack);
        int moveIndex = this.getMoveUpIndex(pack);
        return index > 0 && moveIndex > -1 && !this.options.isFixed(this.packs.get(moveIndex));
    }

    public boolean canMoveDown(Pack pack) {
        if (!this.canMove(pack)) return false;

        int size = this.packs.size();
        if (this.isSelected(pack)) {
            List<Pack> selection = this.getOrderedSelection();
            if (selection.size() > 1) {
                int index = this.packs.indexOf(selection.getLast());
                int moveIndex = index > -1 ? this.getMoveDownIndex(pack) : -1;
                return index > -1 && index < size - 1 && moveIndex > -1 && !this.options.isFixed(this.packs.get(moveIndex));
            }
        }

        int index = this.packs.indexOf(pack);
        int moveIndex = this.getMoveDownIndex(pack);
        return index > -1 && index < size - 1 && moveIndex > -1 && !this.options.isFixed(this.packs.get(moveIndex));
    }

    private boolean canMove(Pack pack) {
        return !this.options.isLocked() && !this.options.isFixed(pack) && !this.isQueried();
    }

    private int getMoveUpIndex(Pack pack) {
        for (int i = this.packs.indexOf(pack) - 1; i >= 0; i--) {
            Pack nextPack = this.packs.get(i);
            if (this.options.isFixed(nextPack)) {
                return -1;
            }
            if (!this.options.isHidden(nextPack)) {
                return i;
            }
        }
        return -1;
    }

    private int getMoveDownIndex(Pack pack) {
        for (int i = this.packs.indexOf(pack) + 1; i < this.packs.size(); i++) {
            Pack nextPack = this.packs.get(i);
            if (this.options.isFixed(nextPack)) {
                return -1;
            }
            if (!this.options.isHidden(nextPack)) {
                return i;
            }
        }
        return -1;
    }

    public int clampPosition(int index) {
        if (index == -1) {
            int minIndex = 0;
            for (int i = 0; i < this.packs.size(); i++) {
                Pack pack = this.packs.get(i);
                if (this.options.isFixed(pack) && this.options.getPosition(pack) == Pack.Position.TOP) {
                    minIndex = i + 1;
                }
            }
            return minIndex;
        }
        return index;
    }

    public boolean isValidInsertPosition(int index, List<Pack> selection) {
        int[] indices = this.getVisibilityIndices(selection);
        if (indices.length == 0) return false;
        if (IntsUtil.hasGap(indices)) return true;
        Arrays.sort(indices);

        int lastSelectionIndex = indices[indices.length - 1];
        if (!this.packs.isEmpty()) {
            int lastItemIndex = this.packs.indexOf(this.packs.getLast());
            if (index == -1 && lastItemIndex == lastSelectionIndex) {
                return false;
            }
        }

        return index != indices[0] && index - 1 != lastSelectionIndex;
    }

    public boolean isValidDropPosition(int index) {
        if (index < 0 || index > this.packs.size()) return false;

        int minDropIndex = 0;
        int maxDropIndex = this.packs.size();
        for (int i = 0; i < this.packs.size(); i++) {
            Pack pack = this.packs.get(i);
            if (this.options.isFixed(pack)) {
                switch (this.options.getPosition(pack)) {
                    case TOP -> minDropIndex = i + 1;
                    case BOTTOM -> maxDropIndex = Math.min(i, maxDropIndex);
                }
            }
        }

        return index >= minDropIndex && index <= maxDropIndex;
    }

    private int[] getVisibilityIndices(List<Pack> selection) {
        int[] selectionIndices = new int[selection.size()];
        for (int i = 0; i < selection.size(); i++) {
            int index = this.visiblePacks.indexOf(selection.get(i));
            selectionIndices[i] = index;
        }
        return selectionIndices;
    }

    @Override
    public void replaceState(@NonNull Snapshot snapshot) {
        this.replaceAll(snapshot.packs);
        this.clearSelection();
        this.selectedPacks.addAll(snapshot.selection);
        this.query = new Query(snapshot.query);
        this.refresh();
    }

    @Override
    public @NonNull Snapshot captureState(String eventName) {
        return new Snapshot(this, List.copyOf(this.packs), List.copyOf(this.selectedPacks), new Query(this.query));
    }

    public record Snapshot(
            PackListModel target,
            List<Pack> packs,
            List<Pack> selection,
            Query query
    ) implements Restorable.Snapshot<Snapshot> {
        public Snapshot retainAll(Set<Pack> validPacks) {
            List<Pack> packs = new ObjectArrayList<>(this.packs.size());
            CollectionsUtil.addIf(packs, this.packs, validPacks::contains);
            return new Snapshot(this.target, List.copyOf(packs), this.selection, this.query);
        }

        public Snapshot replaceAll(List<Pack> packs) {
            return new Snapshot(this.target, List.copyOf(packs), this.selection, this.query);
        }
    }
}
