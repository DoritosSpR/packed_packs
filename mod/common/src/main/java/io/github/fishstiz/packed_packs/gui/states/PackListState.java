package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.gui.components.pack.Query;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.*;

public record PackListState(
        List<Pack> packs,
        List<Pack> visiblePacks,
        SequencedCollection<Pack> selectedPacks,
        Query query,
        @Nullable FolderState folder
) {
    public static PackListState empty() {
        return new PackListState(Collections.emptyList(), Collections.emptyList(), Collections.emptySortedSet(), new Query(), null);
    }

    public PackListState(List<Pack> packs) {
        this(packs, List.copyOf(packs), Collections.emptyList(), new Query(), null);
    }

    public PackListState with(List<Pack> newPacks, SequencedCollection<Pack> newSelection, Query query, PackOptions options) {
        List<Pack> newVisiblePacks = processQuery(newPacks, query, options);
        SequencedSet<Pack> newSelectedPacks = new LinkedHashSet<>(newSelection.size());
        for (Pack pack : newSelection) {
            if (newVisiblePacks.contains(pack)) newSelectedPacks.add(pack);
        }
        return new PackListState(newPacks, newVisiblePacks, Collections.unmodifiableSequencedSet(newSelectedPacks), query, this.folder);
    }

    public PackListState with(List<Pack> newPacks, SequencedCollection<Pack> newSelection, PackOptions options) {
        return this.with(newPacks, newSelection, this.query, options);
    }

    public PackListState withPacks(List<Pack> newPacks, PackOptions options) {
        return this.with(newPacks, this.selectedPacks, options);
    }

    public PackListState withQuery(Query query, PackOptions options) {
        return this.with(this.packs, this.selectedPacks, query, options);
    }

    public PackListState withSelection(SequencedCollection<Pack> newSelection) {
        SequencedSet<Pack> newSelectedPacks = new LinkedHashSet<>(newSelection);
        newSelectedPacks.retainAll(this.visiblePacks);
        return new PackListState(this.packs, this.visiblePacks, Collections.unmodifiableSequencedSet(newSelectedPacks), this.query, this.folder);
    }

    public PackListState withFolder(@Nullable FolderState newFolder) {
        return new PackListState(this.packs, this.visiblePacks, this.selectedPacks, this.query, newFolder);
    }

    private static List<Pack> processQuery(List<Pack> sourcePacks, Query query, PackOptions options) {
        List<Pack> filtered = new ArrayList<>(sourcePacks.size());
        for (Pack pack : sourcePacks) {
            if ((Config.get().isDevMode() /* whatever */ || !options.isHidden(pack)) && query.test(pack)) {
                filtered.add(pack);
            }
        }
        if (query.sort() != null) {
            filtered.sort(query);
        }
        return List.copyOf(filtered);
    }
}
