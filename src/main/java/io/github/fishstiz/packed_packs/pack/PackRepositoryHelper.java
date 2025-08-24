package io.github.fishstiz.packed_packs.pack;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Runnables;
import io.github.fishstiz.packed_packs.config.Folder;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.transform.interfaces.IPack;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.lang.CollectionsUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.apache.commons.lang3.function.Consumers;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PackRepositoryHelper implements PackAssets {
    private final Map<String, ResourceLocation> cachedIcons = new Object2ObjectOpenHashMap<>();
    private final Map<String, Pack> availablePacks = new Object2ObjectLinkedOpenHashMap<>();
    private final Map<String, List<Pack>> folderPacks = new Object2ObjectOpenHashMap<>();
    private final Map<String, CompletableFuture<Folder>> folderConfigs = new Object2ObjectOpenHashMap<>();
    private final PackRepository repository;
    private final Path packDir;
    private final PackSelectionModel model;
    private final boolean resourcePacks;
    private Map<String, ResourceLocation> staleIcons;

    public PackRepositoryHelper(PackRepository repository, Path packDir) {
        this.repository = repository;
        this.packDir = packDir;

        // Fabric API workaround
        this.model = new PackSelectionModel(Runnables.doNothing(), PackAssets::getDefaultIcon, this.repository, Consumers.nop());

        this.resourcePacks = this.repository == Minecraft.getInstance().getResourcePackRepository();

        this.regenerateAvailablePacks();
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
                pack.getDefaultPosition().insert(required, pack, Pack::selectionConfig, true);
            } else {
                optional.add(pack);
            }
        }

        return PackGroup.of(required, optional);
    }

    public PackGroup getPacksBySelected() {
        return this.validateAndGroupPacks(this.getUnselectedPacks(), this.getSelectedPacks());
    }

    /**
     * @param unselected ungrouped list of unselected packs
     * @param selected   ungrouped list of selected packs
     * @return validated and grouped list of packs
     */
    public PackGroup validateAndGroupPacks(List<Pack> unselected, List<Pack> selected) {
        return validatePacks(this.groupByFolders(unselected), this.groupByFolders(selected));
    }

    private void addValidPacks(List<Pack> source, Set<Pack> seen, ObjectOpenHashSet<Pack> validPacks, List<Pack> target) {
        for (Pack pack : source) {
            Pack validPack = validPacks.get(pack); // metadata can change
            // Folder packs can show up in both columns in case user messes around in original screen. PackListBase ignores duplicates anyway.
            if (validPack != null && (seen.add(pack) || pack instanceof FolderPack)) {
                target.add(validPack);
            }
        }
    }

    /**
     * @param unselected grouped list of unselected packs
     * @param selected   grouped list of selected packs
     * @return validated and grouped list of packs
     */
    public PackGroup validatePacks(List<Pack> unselected, List<Pack> selected) {
        Set<Pack> seen = new ObjectOpenHashSet<>();
        ObjectOpenHashSet<Pack> validPacks = new ObjectOpenHashSet<>(this.availablePacks.values());
        List<Pack> validSelected = new ArrayList<>(selected.size());
        List<Pack> validUnselected = new ArrayList<>(unselected.size());

        this.addValidPacks(selected, seen, validPacks, validSelected);
        this.addValidPacks(unselected, seen, validPacks, validUnselected);

        for (Pack validPack : validPacks) {
            if (seen.add(validPack)) {
                if (validPack.isRequired()) {
                    validPack.getDefaultPosition().insert(validSelected, validPack, Pack::selectionConfig, true);
                } else {
                    validUnselected.add(validPack);
                }
            }
        }
        return PackGroup.of(validSelected, validUnselected);
    }

    /**
     * @param folderPack   the folder pack
     * @param orderedPacks the nested packs that define the preferred order
     * @return a validated and ordered list of all packs under the folder pack
     */
    public List<Pack> validateAndOrderNestedPacks(FolderPack folderPack, List<Pack> orderedPacks) {
        List<Pack> orderedValidPacks = this.folderPacks.get(folderPack.getId());
        ObjectOpenHashSet<Pack> validPacks = new ObjectOpenHashSet<>(orderedValidPacks);
        Set<Pack> seen = new ObjectOpenHashSet<>();
        List<Pack> finalOrderedPacks = new ArrayList<>();

        this.addValidPacks(orderedPacks, seen, validPacks, finalOrderedPacks);

        for (Pack validPack : orderedValidPacks) {
            if (seen.add(validPack)) {
                finalOrderedPacks.add(validPack);
            }
        }

        this.folderPacks.put(folderPack.getId(), finalOrderedPacks);
        return finalOrderedPacks;
    }

    public List<Pack> validateAndOrderNestedPackIds(FolderPack folderPack, List<String> orderedPacks) {
        return this.validateAndOrderNestedPacks(folderPack, this.getPacksById(orderedPacks, this.folderPacks.get(folderPack.getId())));
    }

    public List<Pack> getPacksById(List<String> packIds, Map<String, Pack> source) {
        return CollectionsUtil.lookup(packIds, source);
    }

    public List<Pack> getPacksById(List<String> packIds, List<Pack> source) {
        return CollectionsUtil.lookup(packIds, CollectionsUtil.toMap(source, Pack::getId));
    }

    public List<Pack> getPacksById(List<String> packIds) {
        return this.getPacksById(packIds, this.availablePacks);
    }

    /**
     * @param packs ungrouped collection of packs
     */
    private void populateAvailablePacks(Collection<Pack> packs) {
        for (Pack pack : packs) {
            IPack _pack = (IPack) pack;
            if (_pack.packed_packs$nestedPack()) {
                Path folderPath = Objects.requireNonNull(_pack.packed_packs$getPath()).getParent();
                String folderName = PackUtil.generatePackName(folderPath);
                String additionalPrefix = _pack.packed_packs$getAdditionalPrefix();
                String folderId = PackUtil.generatePackId(folderName, additionalPrefix);
                if (!this.availablePacks.containsKey(folderId)) {
                    FolderPack folderPack = new FolderPack(folderId, folderName, additionalPrefix, folderPath);
                    this.folderConfigs.put(folderId, folderPack.loadConfig());
                    this.availablePacks.put(folderId, folderPack);
                }
                this.folderPacks.computeIfAbsent(folderId, id -> new ArrayList<>()).add(pack);
            } else {
                this.availablePacks.put(pack.getId(), pack);
            }
        }
    }

    private void regenerateAvailablePacks() {
        this.availablePacks.clear();
        this.folderPacks.clear();
        this.folderConfigs.clear();
        this.populateAvailablePacks(this.getSelectedPacks());
        this.populateAvailablePacks(this.getUnselectedPacks());
    }

    public void refresh() {
        ((PackSelectionModelAccessor) this.model).packed_packs$reset();
        this.model.findNewPacks();
        this.regenerateAvailablePacks();
    }

    /**
     * @param selected grouped list of selected packs
     */
    public void selectPacks(List<Pack> selected) {
        this.repository.setSelected(Lists.reverse(this.flattenPacks(selected)).stream().map(Pack::getId).collect(ImmutableList.toImmutableList()));
    }

    /**
     * @param groupedPacks grouped list of packs
     * @return flattened list of packs
     */
    public List<Pack> flattenPacks(List<Pack> groupedPacks) {
        if (this.folderPacks.isEmpty()) return groupedPacks;

        List<Pack> flattened = new ArrayList<>(groupedPacks);
        for (int i = flattened.size() - 1; i >= 0; i--) {
            if (flattened.get(i) instanceof FolderPack folderPack) {
                flattened.remove(i);
                List<Pack> nested = this.getNestedPacks(folderPack);
                if (nested != null) {
                    for (Pack pack : Lists.reverse(nested)) {
                        flattened.add(i, pack);
                    }
                }
            }
        }
        return flattened;
    }

    /**
     * @param flatPacks ungrouped list of packs
     * @return grouped list of packs
     */
    public List<Pack> groupByFolders(List<Pack> flatPacks) {
        if (this.folderPacks.isEmpty()) return flatPacks;

        Map<String, String> packToFolder = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<String, List<Pack>> entry : this.folderPacks.entrySet()) {
            for (Pack pack : entry.getValue()) {
                packToFolder.put(pack.getId(), entry.getKey());
            }
        }

        Set<String> seenFolders = new ObjectOpenHashSet<>();
        List<Pack> grouped = new ArrayList<>(flatPacks.size());

        for (Pack pack : flatPacks) {
            String folderId = packToFolder.get(pack.getId());
            if (folderId != null) {
                if (seenFolders.add(folderId)) {
                    grouped.add(this.availablePacks.get(folderId));
                }
            } else {
                grouped.add(pack);
            }
        }
        return grouped;
    }

    public void openDir() {
        Util.getPlatform().openPath(this.packDir);
    }

    @Override
    public void getOrLoadIcon(Pack pack, Consumer<ResourceLocation> iconCallback) {
        if (this.staleIcons != null) {
            ResourceLocation staleIcon = this.staleIcons.get(pack.getId());
            if (staleIcon != null) {
                iconCallback.accept(staleIcon);
            }
        }

        ResourceLocation cachedIcon = this.cachedIcons.get(pack.getId());
        if (cachedIcon != null) {
            iconCallback.accept(cachedIcon);
        } else {
            PackAssets.loadPackIcon(pack).thenAcceptAsync(location -> {
                this.cachedIcons.put(pack.getId(), location);
                iconCallback.accept(location);
            }, Minecraft.getInstance());
        }
    }

    public void clearIconCache() {
        this.staleIcons = new Object2ObjectOpenHashMap<>(this.cachedIcons);
        this.cachedIcons.clear();
    }

    @Override
    public boolean isResourcePacks() {
        return this.resourcePacks;
    }

    @Override
    public boolean isEnabled(Pack pack) {
        Set<String> selectedIds = new ObjectOpenHashSet<>(this.repository.getSelectedIds());

        if (pack instanceof FolderPack folderPack) {
            try {
                for (Pack nestedPack : this.getNestedPacks(folderPack)) {
                    if (selectedIds.contains(nestedPack.getId())) {
                        return true;
                    }
                }
            } catch (NullPointerException ignore) {
            }
            return false;
        }

        return selectedIds.contains(pack.getId());
    }

    @Override
    public Path getDir() {
        return this.packDir;
    }

    public @Nullable Folder getFolderConfig(FolderPack folderPack) {
        CompletableFuture<Folder> future = this.folderConfigs.get(folderPack.getId());
        return future != null ? future.join() : null;
    }

    public List<Pack> getNestedPacks(FolderPack folderPack) {
        return this.validateAndOrderNestedPackIds(folderPack, Objects.requireNonNull(this.getFolderConfig(folderPack)).getPackIds());
    }

    public record PackGroup(ImmutableList<Pack> selected, ImmutableList<Pack> unselected) {
        private static PackGroup of(List<Pack> selected, List<Pack> unselected) {
            return new PackGroup(ImmutableList.copyOf(selected), ImmutableList.copyOf(unselected));
        }
    }
}
