package io.github.fishstiz.packed_packs.gui.intents;

import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.Intent;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.List;

public sealed interface ProfileIntent extends Intent {
    Profile profile();

    @Override
    default boolean record() {
        return false;
    }

    record Select(@Nullable Profile profile, List<Pack> available, List<Pack> enabled) implements ProfileIntent {
    }

    record Rename(Profile profile, String name) implements ProfileIntent {
    }

    record Delete(Profile profile) implements ProfileIntent {
    }

    record Copy(Profile profile, List<Pack> packs) implements ProfileIntent {
    }

    record ToggleLock(Profile profile) implements ProfileIntent {
    }

    record ToggleDefault(Profile profile) implements ProfileIntent {
    }
}
