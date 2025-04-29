package io.github.fishstiz.packed_packs.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionModelAccessor;
import io.github.fishstiz.packed_packs.util.pack.PackIconCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class PackRepositoryHelper {
    private final Map<String, ResourceLocation> cachedIcons = new HashMap<>();
    private final PackRepository repository;
    private final PackSelectionModel model;
    private final List<Pack> availablePacks = new ArrayList<>();

    @SuppressWarnings("ConstantConditions")
    public PackRepositoryHelper(PackRepository repository) {
        this.repository = repository;

        this.model = new PackSelectionModel(PackRepositoryHelper::_update, PackRepositoryHelper::_getIcon, repository, PackRepositoryHelper::_apply);
        this.availablePacks.addAll(((PackSelectionModelAccessor) this.model).getSelectedPacks());
        this.availablePacks.addAll(((PackSelectionModelAccessor) this.model).getUnselectedPacks());
    }

    public @Unmodifiable List<Pack> getPacks() {
        return List.copyOf(this.availablePacks);
    }

    public void refresh() {
        this.model.findNewPacks();
        this.availablePacks.clear();
        this.availablePacks.addAll(((PackSelectionModelAccessor) this.model).getSelectedPacks());
        this.availablePacks.addAll(((PackSelectionModelAccessor) this.model).getUnselectedPacks());
    }

    public void applyPacks(List<Pack> selected) {
        this.repository.setSelected(Lists.reverse(selected).stream().map(Pack::getId).collect(ImmutableList.toImmutableList()));
        Minecraft.getInstance().options.updateResourcePacks(this.repository);
    }

    public ResourceLocation getPackIcon(Pack pack) {
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

    private static ResourceLocation _getIcon(Pack pack) {
        return PackIconCache.DEFAULT_ICON; // placeholder
    }
}
