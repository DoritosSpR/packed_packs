package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FileRenameCloseEvent extends PackListEvent {
    private final Pack trigger;

    public FileRenameCloseEvent(@NotNull PackList target, Pack renamed) {
        super(Objects.requireNonNull(target));

        this.trigger = renamed;
    }

    public Pack trigger() {
        return this.trigger;
    }

    @Override
    public boolean pushToHistory() {
        return false;
    }
}
