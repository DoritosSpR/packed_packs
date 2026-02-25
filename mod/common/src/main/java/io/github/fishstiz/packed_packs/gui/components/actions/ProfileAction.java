package io.github.fishstiz.packed_packs.gui.components.actions;

import io.github.fishstiz.packed_packs.config.Profile;
import org.jspecify.annotations.Nullable;

public sealed interface ProfileAction extends Action {
    @Override
    default boolean pushToHistory() {
        return false;
    }

    default boolean shouldRefresh() {
        return true;
    }

    Profile profile();

    record Select(@Nullable Profile profile) implements ProfileAction {
    }

    record Rename(Profile profile, String name) implements ProfileAction {
        @Override
        public boolean shouldRefresh() {
            return false;
        }
    }

    record Delete(Profile profile) implements ProfileAction {
    }

    record Copy(@Nullable Profile profile) implements ProfileAction {
    }

    record ToggleLock(Profile profile) implements ProfileAction {
    }

    record ToggleDefault(Profile profile) implements ProfileAction {
    }
}
