package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import com.google.common.util.concurrent.Runnables;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class MenuItemBuilder<B extends MenuItemBuilder<B>> {
    protected final Component text;
    protected Runnable action = Runnables.doNothing();
    protected RenderableRect background;
    protected Sprite icon;
    protected boolean shouldAutoSeparate = true;
    protected BooleanSupplier activeSupplier;
    protected IntSupplier textColorSupplier;

    protected MenuItemBuilder(Component text) {
        this.text = text;
    }

    @SuppressWarnings("unchecked")
    protected B self() {
        return (B) this;
    }

    public B action(Runnable action) {
        this.action = action;
        return this.self();
    }

    public B background(@Nullable RenderableRect background) {
        this.background = background;
        return this.self();
    }

    public B background(int backgroundColor) {
        this.background = new ColoredRect(backgroundColor);
        return this.self();
    }

    public B icon(@Nullable Sprite icon) {
        this.icon = icon;
        return this.self();
    }

    public B autoSeparate(boolean value) {
        this.shouldAutoSeparate = value;
        return this.self();
    }

    public B activeWhen(BooleanSupplier activeSupplier) {
        this.activeSupplier = activeSupplier;
        return this.self();
    }

    public B textColor(IntSupplier textColorSupplier) {
        this.textColorSupplier = textColorSupplier;
        return this.self();
    }

    public B textColor(int textColor) {
        this.textColorSupplier = () -> textColor;
        return this.self();
    }

    protected void setDefaults() {
        if (this.activeSupplier == null) {
            this.activeSupplier = () -> true;
        }
        if (this.textColorSupplier == null) {
            this.textColorSupplier = () -> this.activeSupplier.getAsBoolean() ? ARGB.white(1) : ContextMenu.DEFAULT_TEXT_INACTIVE_COLOR;
        }
    }

    public MenuItem build() {
        this.setDefaults();

        return new MenuItemImpl(
                this.text,
                this.action,
                this.background,
                this.icon,
                this.shouldAutoSeparate,
                this.activeSupplier,
                this.textColorSupplier
        );
    }

    private record MenuItemImpl(
            Component text,
            Runnable action,
            RenderableRect background,
            Sprite icon,
            boolean shouldAutoSeparate,
            BooleanSupplier activeSupplier,
            IntSupplier textColorSupplier
    ) implements MenuItem {
        @Override
        public boolean active() {
            return this.activeSupplier.getAsBoolean();
        }

        @Override
        public int textColor() {
            return this.textColorSupplier.getAsInt();
        }
    }
}
