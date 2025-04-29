package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.gui.Metadata;
import io.github.fishstiz.fidgetz.gui.WidgetBuilder;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class CyclicButton<T extends CyclicButton.Option, E> extends Button implements Metadata<E> {
    private final @Nullable Component prefix;
    private final Consumer<T> pressAction;
    private final T[] options;
    private int value = 0;
    private E metadata;

    private CyclicButton(Builder<T, E> builder) {
        super(
                builder.x,
                builder.y,
                builder.width,
                builder.height,
                builder.prefix != null ? builder.prefix : Component.empty(),
                null,
                Button.DEFAULT_NARRATION
        );

        this.pressAction = builder.pressAction;
        this.options = builder.options;
        this.prefix = builder.prefix;
        this.metadata = builder.metadata;

        this.updateMessage();
        if (builder.tooltip != null) this.setTooltip(builder.tooltip);
    }

    @Override
    public void onPress() {
        this.value = this.value >= this.options.length - 1 ? 0 : this.value + 1;
        this.updateMessage();
        if (this.pressAction != null) this.pressAction.accept(this.options[this.value]);
    }

    private void updateMessage() {
        this.setMessage(this.prefix != null
                ? this.prefix.copy().append(": ").append(this.options[this.value].text())
                : this.options[this.value].text()
        );
    }

    @Override
    public E getMetadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(E metadata) {
        this.metadata = metadata;
    }

    public static <E> Builder<Option, E> builder(Consumer<Option> pressAction, Component... components) {
        Option[] options = new Option[components.length];

        for (int i = 0; i < components.length; i++) {
            options[i] = Option.create(components[i]);
        }

        return new Builder<>(pressAction, options);
    }

    public static <T extends Option, E> Builder<T, E> builder(Consumer<T> pressAction, T[] options) {
        return new Builder<>(pressAction, options);
    }

    public static class Builder<T extends Option, E> implements WidgetBuilder<Builder<T, E>> {
        private final T[] options;
        private final Consumer<T> pressAction;
        private int x = 0;
        private int y = 0;
        private int width = WidgetBuilder.DEFAULT_WIDTH;
        private int height = WidgetBuilder.DEFAULT_HEIGHT;
        private Component prefix;
        private Tooltip tooltip;
        private E metadata;

        private Builder(Consumer<T> pressAction, T[] options) {
            this.options = options;
            this.pressAction = pressAction;
        }

        @Override
        public @NotNull Builder<T, E> setWidth(int width) {
            this.width = width;
            return this;
        }

        @Override
        public @NotNull Builder<T, E> setHeight(int height) {
            this.height = height;
            return this;
        }

        @Override
        public @NotNull Builder<T, E> setX(int x) {
            this.x = x;
            return this;
        }

        @Override
        public @NotNull Builder<T, E> setY(int y) {
            this.y = y;
            return this;
        }

        @Override
        public @NotNull Builder<T, E> setDimensions(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        @Override
        public @NotNull Builder<T, E> setPosition(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder<T, E> setPrefix(Component prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder<T, E> setTooltip(Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder<T, E> setMetadata(E metadata) {
            this.metadata = metadata;
            return this;
        }

        public CyclicButton<T, E> build() {
            return new CyclicButton<>(this);
        }
    }

    @FunctionalInterface
    public interface Option {
        @NotNull Component text();

        static Option create(Component component) {
            return () -> component;
        }
    }
}
