package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.CyclicButton;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

public record Query(
        boolean hideIncompatible,
        SortOption sort,
        String search,
        String unmodifiedSearch
) implements Predicate<Pack>, Comparator<Pack> {
    public Query {
        search = search != null ? search.toLowerCase(Locale.ROOT) : null;
    }

    public Query(Query query) {
        this(query.hideIncompatible, query.sort, query.search, query.unmodifiedSearch);
    }

    Query() {
        this(false, null, null, null);
    }

    public Query withHideIncompatible(boolean hideIncompatible) {
        if (this.hideIncompatible == hideIncompatible) return this;
        return new Query(hideIncompatible, this.sort, this.search, this.unmodifiedSearch);
    }

    public Query withSort(SortOption sort) {
        if (Objects.equals(this.sort, sort)) return this;
        return new Query(this.hideIncompatible, sort, this.search, this.unmodifiedSearch);
    }

    public Query withSearch(String search) {
        String searchLower = search != null ? search.toLowerCase(Locale.ROOT) : null;
        if (Objects.equals(this.search, searchLower)) return this;
        return new Query(this.hideIncompatible, this.sort, searchLower, search);
    }

    @Override
    public boolean test(Pack pack) {
        if (pack == null) {
            return false;
        }
        if (this.hideIncompatible && !pack.getCompatibility().isCompatible()) {
            return false;
        }
        if (this.search != null && !normalizeTitle(pack.getTitle().getString()).toLowerCase(Locale.ROOT).contains(this.search)) {
            return false;
        }
        return true;
    }

    @Override
    public int compare(Pack first, Pack second) {
        return this.sort != null ? this.sort.comparator.compare(first, second) : 0;
    }

    boolean hasQuery() {
        return this.hideIncompatible || (this.search != null && !this.search.isEmpty()) || this.sort != null;
    }

    public enum SortOption implements CyclicButton.SpriteOption {
        VANILLA("sort.vanilla", "sort_vanilla", (first, second) -> {
            boolean builtInFirst = PackUtil.isBuiltIn(first);
            boolean builtInSecond = PackUtil.isBuiltIn(second);
            if (builtInFirst != builtInSecond) return builtInFirst ? 1 : -1;

            boolean featureFirst = PackUtil.isFeature(first);
            boolean featureSecond = PackUtil.isFeature(second);
            if (featureFirst != featureSecond) return featureFirst ? 1 : -1;

            return first.getTitle().getString().compareTo(second.getTitle().getString());
        }),
        A_Z("sort.a_z", "sort_a_z", Comparator.comparing(
                pack -> normalizeTitle(pack.getTitle().getString()),
                String.CASE_INSENSITIVE_ORDER
        )),
        Z_A("sort.z_a", "sort_z_a", A_Z.comparator.reversed()),
        RECENT("sort.recent", "sort_recent", Comparator.comparingLong(PackUtil::getLastUpdatedEpochMs).reversed()),
        OLDEST("sort.oldest", "sort_oldest", RECENT.comparator.reversed());

        private final Component component;
        private final Tooltip tooltip;
        private final ButtonSprites sprites;
        private final Comparator<Pack> comparator;

        SortOption(String key, String icon, Comparator<Pack> comparator) {
            this.component = ResourceUtil.getText(key);
            this.tooltip = Tooltip.create(this.component);
            this.sprites = ButtonSprites.of(new Sprite(ResourceUtil.getIcon(icon), Size.of16()));
            this.comparator = folderFirst(comparator);
        }

        @Override
        public @NotNull Component text() {
            return this.component;
        }

        @Override
        public @Nullable Tooltip tooltip() {
            return this.tooltip;
        }

        @Override
        public @Nullable ButtonSprites sprites() {
            return this.sprites;
        }

        static Comparator<Pack> folderFirst(Comparator<Pack> base) {
            return Comparator.comparing((Pack pack) -> !(pack instanceof FolderPack)).thenComparing(base);
        }

        public static SortOption getOrDefault(String name) {
            if (name == null) {
                return VANILLA;
            }

            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                return VANILLA;
            }
        }
    }

    private static String normalizeTitle(String title) {
        return title
                .replaceAll("§.", "") // remove formatting
                .trim();
    }
}
