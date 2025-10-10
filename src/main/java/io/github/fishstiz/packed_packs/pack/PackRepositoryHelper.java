package io.github.fishstiz.packed_packs.pack;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Runnables;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Folder;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import io.github.fishstiz.packed_packs.transform.interfaces.FilteredPackSelectionModel;
import io.github.fishstiz.packed_packs.transform.interfaces.IPack;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.transform.mixin.folders.additional.FolderRepositorySourceAccessor;
import io.github.fishstiz.packed_packs.transform.mixin.folders.additional.PackRepositoryAccessor;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.lang.CollectionsUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.apache.commons.lang3.function.Consumers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

public class PackRepositoryHelper implements PackAssets {
    private final Map<String, ResourceLocation> cachedIcons = new Object2ObjectOpenHashMap<>();
    private final Map<String, Pack> availablePacks = new Object2ObjectLinkedOpenHashMap<>();
    private final Map<String, List<Pack>> folderPacks = new Object2ObjectOpenHashMap<>();
    private final Map<String, CompletableFuture<Folder>> folderConfigs = new Object2ObjectOpenHashMap<>();
    private final PackOptionsResolver resolver;
    private final PackRepository repository;
    private final Path packDir;
    private final boolean resourcePacks;
    private PackSelectionModel model;
    private Map<String, ResourceLocation> staleIcons;

    public PackRepositoryHelper(PackRepository repository, Path packDir, Config.Packs config, Supplier<@Nullable Profile> profileSupplier) {
        this.resolver = new PackOptionsResolver(profileSupplier, config);
        this.repository = repository;
        this.packDir = packDir;
        this.resourcePacks = config instanceof Config.ResourcePacks;

        this.refreshModel();
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

    private void refreshModel() {
        this.model = new PackSelectionModel(Runnables.doNothing(), PackAssets::getDefaultIcon, this.repository, Consumers.nop());
        ((FilteredPackSelectionModel) this.model).packed_packs$filterHidden(false);
    }

    public List<Pack> getPacks() {
        return List.copyOf(this.availablePacks.values());
    }

    public List<Pack> getFlattenedPacks() {
        return this.flattenPacks(List.copyOf(this.availablePacks.values()));
    }

    public PackGroup getPacksByRequirement() {
        List<Pack> required = new ObjectArrayList<>();
        List<Pack> optional = new ObjectArrayList<>();

        for (Pack pack : this.availablePacks.values()) {
            if (this.isRequired(pack)) {
                this.getPosition(pack).insert(required, pack, this::getSelectionConfig, true);
            } else {
                optional.add(pack);
            }
        }

        return PackGroup.of(required, optional);
    }

    public PackGroup getPacksBySelected() {
        return this.validateAndGroupPacks(this.getUnselectedPacks(), this.getSelectedPacks());
    }

    private void removePack(Pack pack) {
        this.repository.removePack(pack.getId());
        this.availablePacks.remove(pack.getId());
        try {
            this.getSelectedPacks().remove(pack);
            this.getUnselectedPacks().remove(pack);
        } catch (UnsupportedOperationException e) {
            // just in case
            PackedPacks.LOGGER.warn("[packed_packs] Failed to mutate PackSelectionModel lists. Report this issue to mod author.");
        }
    }

    @Override
    public boolean deletePack(Pack pack) {
        if (!PackAssets.super.deletePack(pack)) {
            return false;
        }
        if (pack instanceof FolderPack) {
            Optional.ofNullable(this.folderPacks.get(pack.getId())).ifPresent(packs -> packs.forEach(this::removePack));
            this.folderPacks.remove(pack.getId());
            this.folderConfigs.remove(pack.getId());
        }
        this.removePack(pack);
        this.regenerateAvailablePacks();
        return true;
    }

    /**
     * @param unselected ungrouped list of unselected packs
     * @param selected   ungrouped list of selected packs
     * @return validated and grouped list of packs
     */
    public PackGroup validateAndGroupPacks(List<Pack> unselected, List<Pack> selected) {
        return this.validatePacks(this.groupByFolders(unselected), this.groupByFolders(selected));
    }

    public void validateOverrides(Pack pack) {
        Profile profile = this.resolver.profileSupplier().get();
        if (profile == null) return;

        Profile defaultProfile = this.getConfig().getDefaultProfile();
        if (defaultProfile != null && defaultProfile.overridesRequired(pack)) {
            return;
        }

        if (profile.overridesRequired(pack) && !profile.isRequired(pack)) {
            profile.setRequired(null, pack);
        }
    }

    private void addValidPacks(List<Pack> source, Set<Pack> seen, ObjectOpenHashSet<Pack> validPacks, List<Pack> target) {
        for (Pack pack : source) {
            this.validateOverrides(pack);
            Pack validPack = validPacks.get(pack); // metadata can change
            if (validPack != null && (seen.add(pack))) {
                target.add(validPack);
            }
        }
    }

    private void addValidPacks(
            List<Pack> source,
            Set<Pack> seen,
            ObjectOpenHashSet<Pack> validPacks,
            List<Pack> targetUnselected,
            List<Pack> targetSelected
    ) {
        for (Pack pack : source) {
            this.validateOverrides(pack);
            Pack validPack = validPacks.get(pack);
            if (validPack != null && seen.add(pack)) {
                if (this.isRequired(validPack)) {
                    this.getPosition(validPack).insert(targetSelected, validPack, this::getSelectionConfig, true);
                } else {
                    targetUnselected.add(validPack);
                }
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
        List<Pack> validSelected = new ObjectArrayList<>(selected.size());
        List<Pack> validUnselected = new ObjectArrayList<>(unselected.size());

        this.addValidPacks(selected, seen, validPacks, validSelected);
        this.addValidPacks(unselected, seen, validPacks, validUnselected, validSelected);

        for (Pack validPack : validPacks) {
            if (seen.add(validPack)) {
                if (this.isRequired(validPack)) {
                    this.getPosition(validPack).insert(validSelected, validPack, this::getSelectionConfig, true);
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
        List<Pack> finalOrderedPacks = new ObjectArrayList<>();

        this.addValidPacks(orderedPacks, seen, validPacks, finalOrderedPacks);

        for (Pack validPack : orderedValidPacks) {
            if (seen.add(validPack)) {
                finalOrderedPacks.add(validPack);
            }
        }

        this.folderPacks.put(folderPack.getId(), finalOrderedPacks);
        return finalOrderedPacks;
    }

    /**
     * @param folderPack   the folder pack
     * @param orderedPacks the nested pack ids that define the preferred order
     * @return a validated and ordered list of all packs under the folder pack
     */
    public List<Pack> validateAndOrderNestedPackIds(FolderPack folderPack, List<String> orderedPacks) {
        return this.validateAndOrderNestedPacks(folderPack, this.getPacksById(orderedPacks, this.folderPacks.get(folderPack.getId())));
    }

    /**
     * @param packIds grouped pack ids
     * @param source  grouped packs
     * @return grouped packs by id
     */
    public List<Pack> getPacksById(Collection<String> packIds, Map<String, Pack> source) {
        return CollectionsUtil.lookup(packIds, source);
    }

    /**
     * @param packIds grouped pack ids
     * @param source  grouped packs
     * @return grouped packs by id
     */
    public List<Pack> getPacksById(Collection<String> packIds, Collection<Pack> source) {
        return CollectionsUtil.lookup(packIds, CollectionsUtil.toMap(source, Pack::getId));
    }

    /**
     * @param packIds grouped pack ids
     * @return grouped packs by id
     */
    public List<Pack> getPacksById(Collection<String> packIds) {
        return this.getPacksById(packIds, this.availablePacks);
    }

    public List<Pack> getPacksByFlattenedIds(Collection<String> packIds) {
        List<Pack> folderPacks = CollectionsUtil.filter(this.availablePacks.values(), FolderPack.class::isInstance, ObjectArrayList::new);
        List<Pack> available = CollectionsUtil.addAll(folderPacks, this.repository.getAvailablePacks());
        return this.groupByFolders(this.getPacksById(packIds, available));
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
                String folderId = PackUtil.generatePackId(folderName);
                if (!this.availablePacks.containsKey(folderId)) {
                    FolderPack folderPack = new FolderPack(folderId, folderName, folderPath);
                    this.folderConfigs.put(folderId, folderPack.loadConfig());
                    this.availablePacks.put(folderId, folderPack);
                }
                this.folderPacks.computeIfAbsent(folderId, id -> new ObjectArrayList<>()).add(pack);
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
        this.refreshModel();
    }

    /**
     * @param groupedPacks grouped list of packs
     * @return flattened list of packs
     */
    @Override
    public List<Pack> flattenPacks(List<Pack> groupedPacks) {
        if (this.folderPacks.isEmpty()) return groupedPacks;

        List<Pack> flattened = new ObjectArrayList<>(groupedPacks);
        for (int i = flattened.size() - 1; i >= 0; i--) {
            if (flattened.get(i) instanceof FolderPack folderPack) {
                // do not remove folder pack; solves headaches
//                flattened.remove(i);
                List<Pack> nested = this.getNestedPacks(folderPack);
                if (nested != null && !nested.isEmpty()) {
                    for (Pack pack : Lists.reverse(nested)) {
                        flattened.add(i + 1, pack); // insert after folder pack
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
        List<Pack> grouped = new ObjectArrayList<>(flatPacks.size());

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
    public boolean isLocked() {
        Profile profile = this.resolver.profileSupplier().get();
        return profile != null && profile.isLocked();
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
    public boolean isHidden(Pack pack) {
        return this.resolver.isHidden(pack);
    }

    @Override
    public boolean isRequired(Pack pack) {
        return this.resolver.isRequiredOrDefault(pack);
    }

    @Override
    public boolean isFixed(Pack pack) {
        return this.resolver.isFixedOrDefault(pack);
    }

    @Override
    public @NotNull Pack.Position getPosition(Pack pack) {
        return this.resolver.getPositionOrDefault(pack);
    }

    @Override
    public @NotNull PackSelectionConfig getSelectionConfig(Pack pack) {
        return this.resolver.getSelectionConfigOrDefault(pack);
    }

    @Override
    public Config.Packs getConfig() {
        return this.resolver.config();
    }

    public Path getBaseDir() {
        return this.packDir;
    }

    public List<Path> getAdditionalDirs() {
        Path normalizedBaseDir = this.getBaseDir().toAbsolutePath().normalize();

        return ((PackRepositoryAccessor) this.repository).packed_packs$getSources().stream()
                .filter(FolderRepositorySourceAccessor.class::isInstance)
                .map(source -> ((FolderRepositorySourceAccessor) source).packed_packs$getFolder())
                .map(path -> path.toAbsolutePath().normalize())
                .filter(path -> !path.equals(normalizedBaseDir))
                .distinct()
                .toList();
    }

    @Override
    public @Nullable Folder getFolderConfig(@Nullable FolderPack folderPack) {
        if (folderPack == null) return null;
        CompletableFuture<Folder> future = this.folderConfigs.get(folderPack.getId());
        return future != null ? future.join() : null;
    }

    @Override
    public @Nullable Profile getProfile() {
        return this.resolver.profileSupplier().get();
    }

    public List<Pack> getNestedPacks(FolderPack folderPack) {
        return this.validateAndOrderNestedPackIds(folderPack, Objects.requireNonNull(this.getFolderConfig(folderPack)).getPackIds());
    }
}
