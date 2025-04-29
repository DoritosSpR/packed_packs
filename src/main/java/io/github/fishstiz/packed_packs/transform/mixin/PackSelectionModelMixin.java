package io.github.fishstiz.packed_packs.transform.mixin;

import io.github.fishstiz.packed_packs.transform.interfaces.IPackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collections;
import java.util.List;

@Mixin(PackSelectionModel.class)
public abstract class PackSelectionModelMixin implements IPackSelectionModel {
    @Shadow
    @Final
    List<Pack> unselected;

    @Shadow
    @Final
    List<Pack> selected;

    @Shadow
    @Final
    private PackRepository repository;

    @Override
    public void packed_packs$reset() {
        this.selected.clear();
        this.selected.addAll(this.repository.getSelectedPacks());
        Collections.reverse(this.selected);

        this.unselected.clear();
        this.unselected.addAll(this.repository.getAvailablePacks());
        this.unselected.removeAll(this.selected);
    }
}
