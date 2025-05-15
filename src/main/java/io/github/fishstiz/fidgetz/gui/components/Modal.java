package io.github.fishstiz.fidgetz.gui.components;

import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.screens.Screen;

import static io.github.fishstiz.fidgetz.util.DrawUtil.DEMO_BACKGROUND;

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
        protected <S extends Screen & ToggleableDialogContainer> Builder(S screen, LayoutWrapper<T> root) {
            super(screen, root);

            this.background = DEMO_BACKGROUND;
        }

        @Override
        public Modal<T> build() {
            return new Modal<>(this);
        }
    }
}
