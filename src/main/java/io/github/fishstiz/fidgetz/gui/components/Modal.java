package io.github.fishstiz.fidgetz.gui.components;

import com.google.common.util.concurrent.Runnables;
import io.github.fishstiz.fidgetz.gui.shapes.Padding;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Function;

import static io.github.fishstiz.fidgetz.util.DrawUtil.DEMO_BACKGROUND;

public class Modal<T extends Layout> extends ToggleableDialog<LayoutWrapper<T>> {
    private static final int MIN_SIZE = 50;

    protected Modal(Builder<T> builder) {
        super(builder);

        this.root().setPadding(builder.padding);

        if (builder.closeAction != null) {
            builder.closeAction.delegate = () -> this.setOpen(false);
        }
    }

    public void repositionElements() {
        this.root().arrangeElements();
        this.root().setX(this.screen.width / 2 - this.root().getWidth() / 2);
        this.root().setY(this.screen.height / 2 - this.root().getHeight() / 2);
    }

    @FunctionalInterface
    public interface CloseAction {
        void closeModal();
    }

    static class CloseActionImpl implements CloseAction {
        Runnable delegate = Runnables.doNothing();

        @Override
        public void closeModal() {
            this.delegate.run();
        }
    }

    public static <S extends Screen & ToggleableDialogContainer, T extends Layout> Builder<T> builder(S screen, T layout) {
        return new Builder<>(screen, new LayoutWrapper<>(layout, MIN_SIZE, MIN_SIZE));
    }

    public static <S extends Screen & ToggleableDialogContainer, T extends Layout> Builder<T> builder(S screen, Function<CloseAction, T> layoutFactory) {
        CloseActionImpl closeAction = new CloseActionImpl();
        Builder<T> builder = new Builder<>(screen, new LayoutWrapper<>(layoutFactory.apply(closeAction), MIN_SIZE, MIN_SIZE));
        builder.closeAction = closeAction;
        return builder;
    }

    public static class Builder<T extends Layout> extends ToggleableDialog.Builder<LayoutWrapper<T>, Builder<T>> {
        protected Padding padding = Padding.empty();
        CloseActionImpl closeAction;

        protected <S extends Screen & ToggleableDialogContainer> Builder(S screen, LayoutWrapper<T> root) {
            super(screen, root);

            this.background = DEMO_BACKGROUND;
        }

        public Builder<T> padding(Padding padding) {
            this.padding = padding;
            return this;
        }

        public Builder<T> padding(int padding) {
            return this.padding(new Padding(padding));
        }

        @Override
        public Modal<T> build() {
            return new Modal<>(this);
        }
    }
}
