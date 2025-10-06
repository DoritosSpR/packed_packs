package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import com.google.common.util.concurrent.Runnables;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface MenuItem {
    MenuItem SEPARATOR = new MenuItem() {
        @Override
        public Component text() {
            return Component.empty();
        }

        @Override
        public Runnable action() {
            return Runnables.doNothing();
        }

        @Override
        public boolean shouldAutoSeparate() {
            return false;
        }

        @Override
        public boolean active() {
            return false;
        }
    };

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
        return this.active() ? Theme.WHITE.getARGB() : ContextMenu.DEFAULT_TEXT_INACTIVE_COLOR;
    }
}
