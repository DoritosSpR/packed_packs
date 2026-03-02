package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.gui.intents.PackListIntent;
import org.jspecify.annotations.Nullable;

public record PackedPacksState(
        PackListState available,
        PackListState enabled,
        ProfilesState profiles,
        @Nullable ActiveAction activeAction
) {
    public static PackedPacksState empty() {
        return new PackedPacksState(PackListState.empty(), PackListState.empty(), ProfilesState.empty());
    }

    public PackedPacksState(PackListState available, PackListState enabled, ProfilesState profiles) {
        this(available, enabled, profiles, null);
    }

    public PackedPacksState withAvailable(PackListState newAvailable) {
        return new PackedPacksState(newAvailable, this.enabled, this.profiles, null);
    }

    public PackedPacksState withEnabled(PackListState newEnabled) {
        return new PackedPacksState(this.available, newEnabled, this.profiles, null);
    }

    public PackedPacksState withPackLists(PackListState available, PackListState enabled) {
        return new PackedPacksState(available, enabled, this.profiles, null);
    }

    public PackedPacksState withProfiles(ProfilesState newProfiles) {
        return new PackedPacksState(this.available, this.enabled, newProfiles, null);
    }

    public PackedPacksState withRenamingPack(PackListIntent.@Nullable OpenRename intent) {
        return new PackedPacksState(this.available, this.enabled, this.profiles, intent != null ? new ActiveAction.RenamingPack(intent) : null);
    }

    public PackedPacksState withEditingAliases(PackListIntent.@Nullable OpenAliases intent) {
        return new PackedPacksState(this.available, this.enabled, this.profiles, intent != null ? new ActiveAction.EditingAliases(intent) : null);
    }

    public PackedPacksState withDragging(PackListIntent.@Nullable Drag intent) {
        return new PackedPacksState(this.available, this.enabled, this.profiles, intent != null ? new ActiveAction.Dragging(intent) : null);
    }

    public PackListIntent.@Nullable OpenRename renamingPack() {
        return this.activeAction instanceof ActiveAction.RenamingPack(PackListIntent.OpenRename ctx) ? ctx : null;
    }

    public PackListIntent.@Nullable OpenAliases editingAliases() {
        return this.activeAction instanceof ActiveAction.EditingAliases(PackListIntent.OpenAliases ctx) ? ctx : null;
    }

    public PackListIntent.@Nullable Drag dragging() {
        return this.activeAction instanceof ActiveAction.Dragging(PackListIntent.Drag ctx) ? ctx : null;
    }

}
