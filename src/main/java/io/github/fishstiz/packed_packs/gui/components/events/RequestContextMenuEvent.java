package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RequestContextMenuEvent extends PackListEvent{
    private final Pack trigger;
    private final double mouseX;
    private final double mouseY;

    public RequestContextMenuEvent(PackList target, Pack trigger, double mouseX, double mouseY) {
        super(target);

        this.trigger = Objects.requireNonNull(trigger);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public @NotNull Pack trigger() {
        return this.trigger;
    }

    public double mouseX() {
        return this.mouseX;
    }

    public double mouseY() {
        return this.mouseY;
    }

    @Override
    public boolean modifiesTarget() {
        return false;
    }
}
