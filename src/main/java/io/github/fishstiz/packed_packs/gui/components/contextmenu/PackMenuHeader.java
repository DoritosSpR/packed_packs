package io.github.fishstiz.packed_packs.gui.components.contextmenu;

import com.google.common.util.concurrent.Runnables;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItem;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;

public record PackMenuHeader(Pack pack, Sprite icon) implements MenuItem {
    private static final RenderableRect BACKGROUND = new ColoredRect(Theme.GRAY_500.getARGB());

    @Override
    public RenderableRect background() {
        return BACKGROUND;
    }

    @Override
    public Component text() {
        return this.pack.getTitle();
    }

    @Override
    public boolean shouldAutoSeparate() {
        return false;
    }

    @Override
    public boolean active() {
        return false;
    }

    @Override
    public Runnable action() {
        return Runnables.doNothing();
    }

    @Override
    public int textColor() {
        return Theme.WHITE.getARGB();
    }
}
