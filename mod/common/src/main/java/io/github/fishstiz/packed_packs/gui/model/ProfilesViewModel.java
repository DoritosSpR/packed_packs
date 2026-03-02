package io.github.fishstiz.packed_packs.gui.model;

import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.config.Profiles;
import io.github.fishstiz.packed_packs.gui.intents.ProfileIntent;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.pack.PackGroup;
import io.github.fishstiz.packed_packs.pack.PackRepositoryManager;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProfilesViewModel {
    public static final Component TITLE_TEXT = ResourceUtil.getText("profile");
    public static final Component NO_PROFILE_TEXT = ResourceUtil.getText("profile.none");
    public static final Component UNNAMED_TEXT = ResourceUtil.getText("profile.unnamed");
    public static final Component COPY_TEXT = ResourceUtil.getText("profile.copy");
    private final PackType packType;
    private final PackRepositoryManager repository;
    private final Consumer<ProfileIntent> dispatch;
    private final Supplier<PackedPacksState> state;

    public ProfilesViewModel(PackType packType, PackRepositoryManager repository, Consumer<ProfileIntent> dispatch, Supplier<PackedPacksState> state) {
        this.packType = packType;
        this.repository = repository;
        this.dispatch = dispatch;
        this.state = state;
    }

    public List<Entry> entries() {
        PackedPacksState currentState = this.state.get();
        Profile defaultProfile = currentState.profiles().defaultProfile();
        List<Profile> profiles = currentState.profiles().profiles();
        List<Entry> entries = new ObjectArrayList<>(profiles.size());
        if (defaultProfile != null) {
            entries.add(new Entry(defaultProfile));
        }
        for (Profile profile : profiles) {
            if (!Objects.equals(defaultProfile, profile)) {
                entries.add(new Entry(profile));
            }
        }
        return entries;
    }

    public @Nullable Profile selectedProfile() {
        return this.state.get().profiles().selectedProfile();
    }

    public @Nullable Profile defaultProfile() {
        return this.state.get().profiles().defaultProfile();
    }

    public void unselect() {
        PackGroup packs = this.repository.getPacksBySelected();
        this.dispatch.accept(new ProfileIntent.Select(null, packs.unselected(), packs.selected()));
    }

    public void copySelected() {
        Profile selectedProfile = this.state.get().profiles().selectedProfile();
        if (selectedProfile != null && selectedProfile.isTemp()) {
            // save if temp so the copied profile can find next available ID
            Profiles.save(this.packType, selectedProfile);
        }

        Profile copiedProfile = selectedProfile != null
                ? selectedProfile.copy()
                : Profiles.create(NO_PROFILE_TEXT + " - " + COPY_TEXT, this.packType);

        this.dispatch.accept(new ProfileIntent.Copy(copiedProfile, this.state.get().enabled().packs()));
    }

    public void renameSelected(String name) {
        String newName = name;
        if (name == null || name.isEmpty()) {
            newName = UNNAMED_TEXT.getString();
        }

        Profile selectedProfile = this.state.get().profiles().selectedProfile();
        if (selectedProfile != null) {
            this.dispatch.accept(new ProfileIntent.Rename(this.state.get().profiles().selectedProfile(), newName));
        }
    }

    public class Entry {
        private final Profile profile;

        protected Entry(Profile profile) {
            this.profile = profile;
        }

        public Component name() {
            return Component.literal(this.profile.getName());
        }

        public boolean isSelected() {
            return Objects.equals(this.profile, ProfilesViewModel.this.state.get().profiles().selectedProfile());
        }

        public boolean isDefault() {
            return Objects.equals(this.profile, ProfilesViewModel.this.state.get().profiles().defaultProfile());
        }

        public boolean isLocked() {
            return this.profile.isLocked();
        }

        public void delete() {
            ProfilesViewModel.this.dispatch.accept(new ProfileIntent.Delete(this.profile));
        }

        public void select() {
            List<Pack> profilePacks = ProfilesViewModel.this.repository.getPacksByFlattenedIds(this.profile.getPackIds());
            PackGroup packs = ProfilesViewModel.this.repository.validatePacks(ProfilesViewModel.this.state.get().available().packs(), profilePacks);
            ProfilesViewModel.this.dispatch.accept(new ProfileIntent.Select(this.profile, packs.unselected(), packs.selected()));
        }

        public void toggleLock() {
            ProfilesViewModel.this.dispatch.accept(new ProfileIntent.ToggleLock(this.profile));
        }

        public void toggleDefault() {
            ProfilesViewModel.this.dispatch.accept(new ProfileIntent.ToggleDefault(this.profile));
        }
    }
}
