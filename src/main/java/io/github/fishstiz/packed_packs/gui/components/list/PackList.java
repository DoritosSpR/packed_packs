package io.github.fishstiz.packed_packs.gui.components.list;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.packed_packs.util.Restorable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    void drop(PackList source, ImmutableList<Pack> selection, double mouseX, double mouseY);

    void renderDroppableZone(GuiGraphics guiGraphics, PackList source, List<Pack> selection, int mouseX, int mouseY, float partialTick);

    @Nullable Entry getSelected();

    @Nullable Entry getEntry(Pack pack);

    default boolean isTransferable(Pack pack) {
        return testNullable(this.getEntry(pack), PackList.Entry::isTransferable);
    }

    @NotNull ImmutableList<Pack> getPacksCopy();

    @NotNull ImmutableList<Pack> getSelectionCopy();

    @NotNull Query getQueryCopy();

    default PackList.Snapshot captureState() {
        return new Snapshot(this, this.getPacksCopy(), this.getSelectionCopy(), this.getQueryCopy());
    }

    interface Entry extends GuiEventListener {
        boolean isTransferable();

        Pack getPack();
    }

    class Snapshot extends Restorable.Snapshot<Snapshot> {
        public final ImmutableList<Pack> packs;
        public final ImmutableList<Pack> selection;
        public final Query query;

        protected Snapshot(PackList target, List<Pack> packs, List<Pack> selection, Query query) {
            super(target);

            this.packs = ImmutableList.copyOf(packs);
            this.selection = ImmutableList.copyOf(selection);
            this.query = query.copy();
        }

        public Snapshot validate(Collection<Pack> validPacks) {
            List<Pack> validated = new ArrayList<>(this.packs);
            validated.retainAll(validPacks);

            return new Snapshot((PackList) this.target, validated, this.selection, query);
        }
    }
}
