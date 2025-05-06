package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Line;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

public class Modal<T extends Layout> extends ToggleableDialog<LayoutWrapper<T>> {
    private static final int MIN_SIZE = 50;

    protected Modal(Builder<T> builder) {
        super(builder);
    }

    public void repositionElements() {
        this.root().setX(this.screen.width / 2 - this.root().getWidth() / 2);
        this.root().setY(this.screen.height / 2 - this.root().getHeight() / 2);
        this.root().arrangeElements();
    }

    public static <S extends Screen & ToggleableDialogContainer, T extends Layout> Builder<T> builder(S screen, T layout) {
        return new Builder<>(screen, new LayoutWrapper<>(layout, MIN_SIZE, MIN_SIZE));
    }

    public static class Builder<T extends Layout> extends ToggleableDialog.Builder<LayoutWrapper<T>, Builder<T>> {
        private static final Sprite DEFAULT_BACKGROUND = new Sprite(
                ResourceLocation.withDefaultNamespace("textures/gui/demo_background.png"),
                Size.square(256),
                Line.zero(247),
                Line.zero(165)
        );

        protected <S extends Screen & ToggleableDialogContainer> Builder(S screen, LayoutWrapper<T> root) {
            super(screen, root);
        }

        @Override
        public Modal<T> build() {
            if (this.background == null) {
                this.background = DEFAULT_BACKGROUND;
            }

            return new Modal<>(this);
        }
    }
}
