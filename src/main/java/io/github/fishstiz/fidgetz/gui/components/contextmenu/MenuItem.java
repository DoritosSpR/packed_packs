package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.ARGBColor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface MenuItem {
    MenuItem SEPARATOR = MenuItem.builder(Component.empty()).autoSeparate(false).build();

    Component text();

    Runnable action();

    default @Nullable RenderableRect background() {
        return null;
    }

    default @Nullable Sprite icon() {
        return null;
    }

    default boolean shouldAutoSeparate() {
        return true;
    }

    default boolean active() {
        return true;
    }

    default int textColor() {
        return this.active() ? ARGBColor.WHITE : ContextMenu.DEFAULT_TEXT_INACTIVE_COLOR;
    }

    static MenuItemBuilder<?> builder(Component text) {
        return new MenuItemBuilder<>(text);
    }
}
