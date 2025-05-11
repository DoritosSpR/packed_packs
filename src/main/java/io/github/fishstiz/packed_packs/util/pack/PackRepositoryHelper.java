package io.github.fishstiz.packed_packs.util.pack;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.github.fishstiz.packed_packs.transform.interfaces.IPackSelectionModel;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class PackRepositoryHelper implements PackIconCache {
    private final Map<String, ResourceLocation> cachedIcons = new HashMap<>();
    private final Map<String, Pack> availablePacks = new LinkedHashMap<>();
    private final PackRepository repository;
    private final Path packDir;
    private final PackSelectionModel model;

    public PackRepositoryHelper(PackRepository repository, Path packDir) {
        this.repository = repository;
        this.packDir = packDir;

        // Fabric API workaround
        this.model = new PackSelectionModel(PackRepositoryHelper::_update, PackRepositoryHelper::_getIcon, this.repository, PackRepositoryHelper::_apply);

        this.populateAvailablePacks();
    }

    public PackRepository getRepository() {
        return this.repository;
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

    public PackGroup validatePacks(List<Pack> unselected, List<Pack> selected) {
        Set<Pack> validPacks = new HashSet<>(this.availablePacks.values());
        Set<Pack> previousUnselected = new HashSet<>(unselected);
        Set<Pack> previousSelected = new HashSet<>(selected);
        List<Pack> unselectedPacks = new ArrayList<>(unselected);
        List<Pack> selectedPacks = new ArrayList<>(selected);

        unselectedPacks.retainAll(validPacks);
        selectedPacks.retainAll(validPacks);

        for (Pack pack : validPacks) {
            if (!previousSelected.contains(pack) && !previousUnselected.contains(pack)) {
                if (pack.isRequired()) {
                    selectedPacks.add(pack);
                } else {
                    unselectedPacks.add(pack);
                }
            }
        }

        unselectedPacks.removeIf(new HashSet<>(selectedPacks)::contains);

        return PackGroup.of(selectedPacks, unselectedPacks);
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
        ((IPackSelectionModel) this.model).packed_packs$reset();
        this.model.findNewPacks();
        this.availablePacks.clear();
        this.populateAvailablePacks();
    }

    public void selectPacks(List<Pack> selected) {
        this.repository.setSelected(Lists.reverse(selected).stream().map(Pack::getId).collect(ImmutableList.toImmutableList()));
    }

    public void openDirectory() {
        Util.getPlatform().openPath(this.packDir);
    }

    @Override
    public void getOrLoad(Pack pack, Consumer<ResourceLocation> iconCallback) {
        ResourceLocation cachedIcon = this.cachedIcons.get(pack.getId());

        if (cachedIcon != null) {
            iconCallback.accept(cachedIcon);
        } else {
            iconCallback.accept(DEFAULT_ICON);
            PackIconCache.loadPackIcon(pack).thenAcceptAsync(location -> {
                this.cachedIcons.put(pack.getId(), location);
                iconCallback.accept(location);
            });
        }
    }

    public record PackGroup(ImmutableList<Pack> selected, ImmutableList<Pack> unselected) {
        private static PackGroup of(List<Pack> selected, List<Pack> unselected) {
            return new PackGroup(ImmutableList.copyOf(selected), ImmutableList.copyOf(unselected));
        }
    }

    private static ResourceLocation _getIcon(Pack pack) {
        return DEFAULT_ICON; // placeholder
    }

    private static void _update() {
        // placeholder
    }

    private static void _apply(PackRepository repository) {
        // placeholder
    }
}
