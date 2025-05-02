package io.github.fishstiz.packed_packs.gui.event;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.packed_packs.gui.components.list.PackList;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public final class RequestTransferEvent extends PackListEvent {
    private final ImmutableList<Pack> payload;
    private final @Nullable Pack trigger;

    public RequestTransferEvent(PackList target, List<Pack> payload, @Nullable Pack trigger) {
        super(target);

        if (payload.isEmpty()) {
            throw new IllegalStateException("Payload cannot be empty");
        }

        this.payload = ImmutableList.copyOf(payload);
        this.trigger = trigger;
    }

    public RequestTransferEvent(PackList target, @NotNull Pack trigger) {
        super(target);

        Objects.requireNonNull(trigger);

        this.payload = ImmutableList.of(trigger);
        this.trigger = trigger;
    }

    @Override
    public boolean modifiesTarget() {
        return true;
    }

    public ImmutableList<Pack> payload() {
        return this.payload;
    }

    public @Nullable Pack trigger() {
        return this.trigger;
    }
}