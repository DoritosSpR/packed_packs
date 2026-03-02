package io.github.fishstiz.packed_packs.gui.states;

import io.github.fishstiz.packed_packs.config.PackOptions;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.pack.PackOptionsResolver;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public record ProfilesState(
        List<Profile> profiles,
        @Nullable Profile selectedProfile,
        @Nullable Profile defaultProfile,
        PackOptions options
) {
    public ProfilesState {
        if (options == null) {
            options = new PackOptionsResolver(this::selectedProfile, this::defaultProfile);
        }
    }

    public ProfilesState(List<Profile> profiles, @Nullable Profile selectedProfile, @Nullable Profile defaultProfile) {
        this(profiles, selectedProfile, defaultProfile, null);
    }

    public static ProfilesState empty() {
        return new ProfilesState(Collections.emptyList(), null, null);
    }

    public ProfilesState withProfiles(List<Profile> newProfiles) {
        Profile newSelectedProfile = newProfiles.contains(selectedProfile) ? selectedProfile : null;
        Profile newDefaultProfile = newProfiles.contains(defaultProfile) ? defaultProfile : null;
        return new ProfilesState(newProfiles, newSelectedProfile, newDefaultProfile);
    }

    public ProfilesState withSelected(@Nullable Profile selectedProfile) {
        return new ProfilesState(profiles, selectedProfile, defaultProfile);
    }

    public ProfilesState withDefault(@Nullable Profile defaultProfile) {
        return new ProfilesState(profiles, selectedProfile, defaultProfile);
    }
}
