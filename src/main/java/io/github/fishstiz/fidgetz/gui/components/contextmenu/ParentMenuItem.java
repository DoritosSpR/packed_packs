package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import com.google.common.util.concurrent.Runnables;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface ParentMenuItem extends MenuItem {
    List<? extends MenuItem> children();

    default Runnable action() {
        return Runnables.doNothing();
    }

    static ParentMenuItemBuilder builder(Component text) {
        return new ParentMenuItemBuilder(text);
    }

    static ParentMenuItemBuilder builder(Component text, List<MenuItem> children) {
        return new ParentMenuItemBuilder(text).addChildren(children);
    }
}
