package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public record SimpleMenuItem(Component text, BooleanSupplier activeSupplier, Runnable action) implements MenuItem {
    public SimpleMenuItem(Component text, Runnable onPress) {
        this(text, ContextMenu.DEFAULT_ACTIVE_SUPPLIER, onPress);
    }

    public SimpleMenuItem(String text, Runnable onPress) {
        this(Component.literal(text), onPress);
    }

    @Override
    public boolean active() {
        return this.activeSupplier.getAsBoolean();
    }
}
