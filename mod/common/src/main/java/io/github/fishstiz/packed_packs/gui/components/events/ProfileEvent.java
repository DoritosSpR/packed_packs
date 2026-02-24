package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.config.Profile;
import org.jspecify.annotations.Nullable;

public sealed interface ProfileEvent extends Action {
    @Override
    default boolean pushToHistory() {
        return false;
    }

    default boolean shouldRefresh() {
        return true;
    }

    Profile profile();

    record Select(@Nullable Profile profile) implements ProfileEvent {
    }

    record Rename(Profile profile, String name) implements ProfileEvent {
        @Override
        public boolean shouldRefresh() {
            return false;
        }
    }

    record Delete(Profile profile) implements ProfileEvent {
    }

    record Copy(@Nullable Profile profile) implements ProfileEvent {
    }

    record ToggleLock(Profile profile) implements ProfileEvent {
    }

    record ToggleDefault(Profile profile) implements ProfileEvent {
    }
}
