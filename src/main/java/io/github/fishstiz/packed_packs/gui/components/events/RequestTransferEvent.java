package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public record RequestTransferEvent(
        PackList target,
        @Nullable Pack trigger,
        List<Pack> payload
) implements PackListEvent {
    public RequestTransferEvent {
        payload = List.copyOf(payload);
    }

    public RequestTransferEvent(PackList target, @NotNull Pack trigger) {
        this(target, Objects.requireNonNull(trigger), List.of(trigger));
    }

    @Override
    public boolean pushToHistory() {
        return true;
    }
}