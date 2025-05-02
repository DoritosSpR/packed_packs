package io.github.fishstiz.packed_packs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.util.pack.PackIconCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PackRepositoryHelper implements PackIconCache {
    private final Map<String, ResourceLocation> cachedIcons = new HashMap<>();
    private final PackRepository repository;
    private final PackSelectionModel model;
    private final Map<String, Pack> availablePacks = new LinkedHashMap<>();

    public PackRepositoryHelper(PackRepository repository) {
        this.repository = repository;

        this.model = new PackSelectionModel(PackRepositoryHelper::_update, this::getIcon, repository, PackRepositoryHelper::_apply);

        this.populateAvailablePacks();
    }

    private List<Pack> getSelectedPacks() {
        return ((PackSelectionModelAccessor) this.model).getSelectedPacks();
    }

    private List<Pack> getUnselectedPacks() {
        return ((PackSelectionModelAccessor) this.model).getUnselectedPacks();
    }

    public ImmutableList<Pack> getPacks() {
        return ImmutableList.copyOf(this.availablePacks.values());
    }

    public PackGroup getPacksByRequirement() {
        List<Pack> required = new ArrayList<>();
        List<Pack> optional = new ArrayList<>();

        for (Pack pack : this.availablePacks.values()) {
            if (pack.isRequired()) {
                required.add(pack);
            } else {
                optional.add(pack);
            }
        }

        return PackGroup.of(required, optional);
    }

    public PackGroup getPacksBySelected() {
        return PackGroup.of(this.getSelectedPacks(), this.getUnselectedPacks());
    }

    public List<Pack> getPacksById(List<String> packIds) {
        List<Pack> packs = new ArrayList<>();

        for (String id : packIds) {
            Pack pack = this.availablePacks.get(id);
            if (pack != null) {
                packs.add(pack);
            }
        }

        return packs;
    }

    private void populateAvailablePacks() {
        for (Pack pack : this.getSelectedPacks()) {
            this.availablePacks.put(pack.getId(), pack);
        }
        for (Pack pack : this.getUnselectedPacks()) {
            this.availablePacks.put(pack.getId(), pack);
        }
    }

    public void refresh() {
        this.model.findNewPacks();
        this.availablePacks.clear();
        this.populateAvailablePacks();
    }

    public void applyPacks(List<Pack> selected) {
        this.repository.setSelected(Lists.reverse(selected).stream().map(Pack::getId).collect(ImmutableList.toImmutableList()));
        Minecraft.getInstance().options.updateResourcePacks(this.repository);
    }

    @Override
    public @NotNull ResourceLocation getIcon(Pack pack) {
        return pack != null
                ? this.cachedIcons.computeIfAbsent(pack.getId(), string -> PackIconCache.loadPackIcon(pack))
                : PackIconCache.DEFAULT_ICON;
    }

    private static void _update() {
        // placeholder
    }

    private static void _apply(PackRepository repository) {
        // placeholder
    }

    public record PackGroup(ImmutableList<Pack> selected, ImmutableList<Pack> unselected) {
        private static PackGroup of(List<Pack> required, List<Pack> optional) {
            return new PackGroup(ImmutableList.copyOf(required), ImmutableList.copyOf(optional));
        }
    }
}
