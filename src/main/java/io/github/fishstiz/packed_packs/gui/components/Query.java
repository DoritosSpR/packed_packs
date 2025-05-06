package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.gui.components.CyclicButton;
import io.github.fishstiz.packed_packs.util.pack.PackUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static io.github.fishstiz.packed_packs.util.ResourceUtil.getText;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingLong;

public class Query {
    private boolean hideIncompatible = false;
    private SortOption sort;
    private String search;

    Query() {
    }

    Query(boolean hideIncompatible, SortOption sort, String search) {
        this.hideIncompatible = hideIncompatible;
        this.sort = sort;
        this.search = search;
    }

    boolean setHideIncompatible(boolean hideIncompatible) {
        boolean updated = this.hideIncompatible != hideIncompatible;
        this.hideIncompatible = hideIncompatible;
        return updated;
    }

    boolean setSort(SortOption sort) {
        boolean updated = !Objects.equals(this.sort, sort);
        this.sort = sort;
        return updated;
    }

    boolean setSearch(String search) {
        boolean updated = !Objects.equals(this.search, search);
        this.search = search;
        return updated;
    }

    boolean update(boolean incompatibleHidden, SortOption sort, String search) {
        boolean updated = this.setHideIncompatible(incompatibleHidden);
        updated |= this.setSort(sort);
        updated |= this.setSearch(search);
        return updated;
    }

    boolean update(Query query) {
        return this.update(query.hideIncompatible, query.sort, query.search);
    }

    void apply(final List<Pack> packs) {
        Objects.requireNonNull(packs);

        if (this.hideIncompatible) {
            packs.removeIf(pack -> !pack.getCompatibility().isCompatible());
        }
        if (this.search != null && !this.search.isEmpty()) {
            packs.removeIf(pack -> !pack.getTitle().getString().toLowerCase().contains(this.search.toLowerCase()));
        }
        if (this.sort != null) {
            packs.sort(this.sort.getComparator());
        }
    }

    boolean isQuerying() {
        return this.hideIncompatible || (this.search != null && !this.search.isEmpty()) || this.sort != null;
    }

    public Query copy() {
        return new Query(this.hideIncompatible, this.sort, this.search);
    }

    public boolean isHideIncompatible() {
        return this.hideIncompatible;
    }

    public SortOption getSort() {
        return this.sort;
    }

    public String getSearch() {
        return this.search;
    }

    public enum SortOption implements CyclicButton.Option {
        A_Z(getText("sort.a_z"), comparing(pack -> pack.getTitle().getString())),
        Z_A(getText("sort.z_a"), A_Z.getComparator().reversed()),
        OLDEST(getText("sort.oldest"), comparingLong(PackUtil::getLastUpdatedEpochMs)),
        RECENT(getText("sort.recent"), OLDEST.getComparator().reversed());

        private final Component component;
        private final Comparator<Pack> comparator;

        SortOption(Component component, Comparator<Pack> comparator) {
            this.component = component;
            this.comparator = comparator;
        }

        @Override
        public @NotNull Component text() {
            return this.component;
        }

        public @NotNull Comparator<Pack> getComparator() {
            return this.comparator;
        }
    }
}
