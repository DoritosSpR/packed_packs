package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import com.google.common.util.concurrent.Runnables;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

public record ParentMenuItem(Component text, List<MenuItem> children) implements MenuItem {
    @Override
    public Runnable action() {
        return Runnables.doNothing();
    }

    public ParentMenuItem(Component text, MenuItem... children) {
        this(text, Arrays.asList(children));
    }
}
