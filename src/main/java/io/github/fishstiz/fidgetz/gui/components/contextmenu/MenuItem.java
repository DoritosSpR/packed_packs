package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import com.google.common.util.concurrent.Runnables;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.network.chat.Component;

public interface MenuItem {
    MenuItem SEPARATOR = new SimpleMenuItem(Component.empty(), Runnables.doNothing());

    Component text();

    Runnable action();

    default boolean active() {
        return true;
    }

    default int textColor() {
        return this.active() ? Theme.WHITE.getARGB() : ContextMenu.DEFAULT_TEXT_INACTIVE_COLOR;
    }
}
