package io.github.fishstiz.packed_packs.gui.components.list;

import io.github.fishstiz.packed_packs.util.Restorable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.github.fishstiz.packed_packs.util.lang.ObjectsUtil.testNullable;

public interface PackList extends ContainerEventHandler, Restorable<PackList.Snapshot> {
    void add(Pack pack);

    void insert(Pack pack, int index);

    boolean move(Pack pack, int index);

    boolean move(List<Pack> packs, int index);

    void remove(Pack pack);

    void unselect(Pack pack);

    void select(Pack pack);

    void selectExclusive(Pack pack);

    void selectRange(Pack pack);

    void clearSelection();

    void drop(PackList source, List<Pack> selection, double mouseX, double mouseY);

    @Nullable Entry getSelected();

    @Nullable Entry getEntry(Pack pack);

    default boolean isTransferable(Pack pack) {
        return testNullable(this.getEntry(pack), PackList.Entry::isTransferable);
    }

    @Unmodifiable
    @NotNull List<Pack> getPacksCopy();

    @Unmodifiable
    @NotNull List<Pack> getSelectionCopy();

    @NotNull Query getQueryCopy();

    default PackList.Snapshot captureState() {
        return new Snapshot(this, this.getPacksCopy(), this.getSelectionCopy(), this.getQueryCopy());
    }

    interface Entry extends GuiEventListener {
        boolean isTransferable();

        Pack getPack();
    }

    class Snapshot extends Restorable.Snapshot<Snapshot> {
        public final @Unmodifiable List<Pack> packs;
        public final @Unmodifiable List<Pack> selection;
        public final Query query;

        protected Snapshot(PackList target, List<Pack> packs, List<Pack> selection, Query query) {
            super(target);

            this.packs = List.copyOf(packs);
            this.selection = List.copyOf(selection);
            this.query = query.copy();
        }

        public Snapshot validate(Collection<Pack> validPacks) {
            List<Pack> validated = new ArrayList<>(this.packs);
            validated.retainAll(validPacks);

            return new Snapshot((PackList) this.target, validated, this.selection, query);
        }
    }
}
