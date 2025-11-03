package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.components.SelectableList;
import io.github.fishstiz.packed_packs.gui.history.Restorable;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntFunction;

public class PackListModel extends SelectableList<Pack> implements Restorable<PackListModel.Snapshot> {
    private final PackOptionsContext options;
    private Query query = new Query();

    public PackListModel(PackOptionsContext options) {
        this.options = options;
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

    @Override
    protected boolean filter(Pack pack) {
        if (!Config.get().isDevMode() && this.options.isHidden(pack)) {
            return false;
        }
        if (!this.query.test(pack)) {
            return false;
        }
        return super.filter(pack);
    }

    @Override
    public void refresh() {
        super.refresh();
        if (this.query.sort() != null) {
            this.visibleItems.sort(this.query);
        }
    }

    @Override
    public void add(Pack pack) {
        if (pack != null && !this.items.contains(pack)) {
            int index = 0;
            for (Pack p : this.items) {
                if (!this.options.isFixed(p) || this.options.getPosition(p) == Pack.Position.BOTTOM) break;
                index++;
            }
            this.items.add(index, pack);
        }
    }

    @Override
    public boolean move(int index, Pack pack) {
        return !this.options.isFixed(pack) && super.move(index, pack);
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
        if (this.items.contains(pack)) {
            int targetIndex = moveIndexFn.applyAsInt(pack);
            return targetIndex > -1 && this.move(targetIndex, pack);
        }
        return false;
    }

    private List<Pack> moveSelection(ToIntFunction<Pack> moveIndexFn, List<Pack> selection) {
        List<Pack> moved = new ObjectArrayList<>();
        Set<Pack> packSet = new ObjectOpenHashSet<>(this.items);
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
                int index = this.items.indexOf(selection.getFirst());
                int moveIndex = index > -1 ? this.getMoveUpIndex(pack) : -1;
                return index > 0 && moveIndex > -1 && !this.options.isFixed(this.items.get(moveIndex));
            }
        }

        int index = this.items.indexOf(pack);
        int moveIndex = this.getMoveUpIndex(pack);
        return index > 0 && moveIndex > -1 && !this.options.isFixed(this.items.get(moveIndex));
    }

    public boolean canMoveDown(Pack pack) {
        if (!this.canMove(pack)) return false;

        int size = this.items.size();
        if (this.isSelected(pack)) {
            List<Pack> selection = this.getOrderedSelection();
            if (selection.size() > 1) {
                int index = this.items.indexOf(selection.getLast());
                int moveIndex = index > -1 ? this.getMoveDownIndex(pack) : -1;
                return index > -1 && index < size - 1 && moveIndex > -1 && !this.options.isFixed(this.items.get(moveIndex));
            }
        }

        int index = this.items.indexOf(pack);
        int moveIndex = this.getMoveDownIndex(pack);
        return index > -1 && index < size - 1 && moveIndex > -1 && !this.options.isFixed(this.items.get(moveIndex));
    }

    private boolean canMove(Pack pack) {
        return !this.options.isLocked() && !this.options.isFixed(pack) && !this.isQueried();
    }

    private int getMoveUpIndex(Pack pack) {
        for (int i = this.items.indexOf(pack) - 1; i >= 0; i--) {
            Pack nextPack = this.items.get(i);
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
        for (int i = this.items.indexOf(pack) + 1; i < this.items.size(); i++) {
            Pack nextPack = this.items.get(i);
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
            for (int i = 0; i < this.items.size(); i++) {
                Pack pack = this.items.get(i);
                if (this.options.isFixed(pack) && this.options.getPosition(pack) == Pack.Position.TOP) {
                    minIndex = i + 1;
                }
            }
            return minIndex;
        }
        return index;
    }

    public boolean isValidDropPosition(int index) {
        if (index < 0 || index > this.items.size()) return false;

        int minDropIndex = 0;
        int maxDropIndex = this.items.size();
        for (int i = 0; i < this.items.size(); i++) {
            Pack pack = this.items.get(i);
            if (this.options.isFixed(pack)) {
                switch (this.options.getPosition(pack)) {
                    case TOP -> minDropIndex = i + 1;
                    case BOTTOM -> maxDropIndex = Math.min(i, maxDropIndex);
                }
            }
        }

        return index >= minDropIndex && index <= maxDropIndex;
    }

    @Override
    public void replaceState(@NotNull Snapshot snapshot) {
        this.replaceAll(snapshot.packs);
        this.clearSelection();
        this.selectedItems.addAll(snapshot.selection);
        this.query = new Query(snapshot.query);
        this.refresh();
    }

    @Override
    public @NotNull Snapshot captureState(String eventName) {
        return new Snapshot(this, List.copyOf(this.items), List.copyOf(this.selectedItems), new Query(this.query));
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
