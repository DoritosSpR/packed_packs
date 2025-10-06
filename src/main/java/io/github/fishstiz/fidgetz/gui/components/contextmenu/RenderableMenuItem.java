package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import net.minecraft.network.chat.Component;

public record RenderableMenuItem(RenderableRect renderer, boolean active, Runnable action) implements MenuItem {
    @Override
    public Component text() {
        return Component.empty();
    }
}
