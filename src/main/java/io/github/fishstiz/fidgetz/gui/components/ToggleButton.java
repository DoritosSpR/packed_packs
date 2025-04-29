package io.github.fishstiz.fidgetz.gui.components;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ToggleButton<E> extends FidgetzButton<E> {
    private final Consumer<Boolean> listener;
    private final boolean prefixMessage;
    private Component prefix;
    private boolean value;

    protected ToggleButton(ToggleBuilder<E> builder) {
        super(builder);

        this.listener = builder.listener;
        this.prefixMessage = builder.prefixMessage;
        this.value = builder.value;
        this.prefix = this.getMessage();

        this.updateMessage();
    }

    public void setValue(boolean value) {
        this.value = value;
        this.updateMessage();

        if (this.listener != null) {
            this.listener.accept(value);
        }
    }

    public boolean getValue() {
        return this.value;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);

        this.setValue(!this.getValue());
    }

    @Override
    public void setMessage(Component message) {
        this.prefix = message;
        this.updateMessage();
    }

    public Component getPrefix() {
        return this.prefix;
    }

    private void updateMessage() {
        Component valueText = this.getValue() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;

        if (this.prefixMessage) {
            super.setMessage(this.getPrefix().copy().append(": ").append(valueText));
        } else {
            super.setMessage(valueText);
        }
    }

    public static <E> ToggleBuilder<E> builder() {
        return new ToggleBuilder<>();
    }

    public static class ToggleBuilder<E> extends FidgetzButton.Builder<E, ToggleBuilder<E>> {
        private boolean value = false;
        private boolean prefixMessage = true;
        private Consumer<Boolean> listener;

        protected ToggleBuilder() {
        }

        public ToggleBuilder<E> setValue(boolean value) {
            this.value = value;
            return this;
        }

        public ToggleBuilder<E> setPrefixMessage(boolean prefixMessage) {
            this.prefixMessage = prefixMessage;
            return this;
        }

        public ToggleBuilder<E> setListener(Consumer<Boolean> listener) {
            this.listener = listener;
            return this;
        }

        @Override
        public ToggleButton<E> build() {
            return new ToggleButton<>(this);
        }
    }
}
