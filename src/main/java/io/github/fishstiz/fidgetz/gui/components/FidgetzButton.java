package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.gui.Metadata;
import io.github.fishstiz.fidgetz.gui.WidgetBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FidgetzButton<E> extends Button implements Metadata<E> {
    private final List<Runnable> listeners = new ArrayList<>();
    private final ButtonSprites sprites;
    private E metadata;

    protected FidgetzButton(Builder<E, ?> builder) {
        super(builder.x, builder.y, builder.width, builder.height, builder.message, builder.onPress, DEFAULT_NARRATION);

        this.metadata = builder.metadata;
        this.sprites = builder.sprites;

        if (builder.tooltip != null) {
            this.setTooltip(builder.tooltip);
        }
    }

    @Override
    public void onPress() {
        super.onPress();

        for (var listener : this.listeners) {
            listener.run();
        }
    }

    public void addListener(Runnable listener) {
        this.listeners.add(listener);
    }

    @Override
    public E getMetadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(E metadata) {
        this.metadata = metadata;
    }

    protected boolean hasSprite() {
        return this.sprites != null;
    }

    protected void renderSprite(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        this.sprites.get(this.active).renderClamped(guiGraphics, x, y, width, height, partialTick);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.isHovered = this.isMouseOver(mouseX, mouseY);

        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        if (this.hasSprite()) {
            int spriteWidth = this.getWidth();
            int spriteHeight = this.getHeight();
            int spriteX = this.getX() + (this.getWidth() - spriteWidth) / 2;
            int spriteY = this.getY() + (this.getHeight() - spriteHeight) / 2;

            this.renderSprite(guiGraphics, spriteX, spriteY, spriteWidth, spriteHeight, partialTick);
        }
    }

    @Override
    public void renderString(GuiGraphics guiGraphics, Font font, int color) {
        if (!this.hasSprite()) {
            super.renderString(guiGraphics, font, color);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        boolean isMouseOver = super.isMouseOver(mouseX, mouseY);
        boolean isCoveredAtPoint = false;

        if (Minecraft.getInstance().screen instanceof ToggleableDialogContainer dialogContainer) {
            isCoveredAtPoint = dialogContainer.isChildCoveredAtPoint(this, mouseX, mouseY);
        }

        return isMouseOver && !isCoveredAtPoint;
    }

    public static <E> Builder<E, ?> builder() {
        return new Builder<>();
    }

    public static class Builder<E, B extends Builder<E, B>> implements WidgetBuilder<Builder<E, B>> {
        private int x = 0;
        private int y = 0;
        private int width = DEFAULT_WIDTH;
        private int height = DEFAULT_HEIGHT;
        private Component message = Component.empty();
        private Tooltip tooltip;
        private ButtonSprites sprites;
        private OnPress onPress = btn -> {
        };
        private E metadata;

        protected Builder() {
        }

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        @Override
        public @NotNull B setX(int x) {
            this.x = x;
            return self();
        }

        @Override
        public @NotNull B setY(int y) {
            this.y = y;
            return self();
        }

        @Override
        public @NotNull B setPosition(int x, int y) {
            this.x = x;
            this.y = y;
            return self();
        }

        @Override
        public @NotNull B setWidth(int width) {
            this.width = width;
            return self();
        }

        @Override
        public @NotNull B setHeight(int height) {
            this.height = height;
            return self();
        }

        @Override
        public @NotNull B setDimensions(int width, int height) {
            this.width = width;
            this.height = height;
            return self();
        }

        public B makeSquare(int size) {
            this.height = size;
            this.width = size;
            return self();
        }

        public B makeSquare() {
            return makeSquare(this.height);
        }

        public B setMessage(Component message) {
            this.message = message;
            return self();
        }

        public B setMessage(String message) {
            return this.setMessage(Component.translatable(message));
        }

        public B setTooltip(Tooltip tooltip) {
            this.tooltip = tooltip;
            return self();
        }

        public B setSpriteOnly(ButtonSprites sprites) {
            this.sprites = sprites;
            return self();
        }

        public B setSpriteOnly(Sprite sprite) {
            this.sprites = ButtonSprites.of(sprite);
            return self();
        }

        public B setOnPress(OnPress onPress) {
            this.onPress = onPress;
            return self();
        }

        public B setOnPress(Runnable onPress) {
            this.onPress = btn -> onPress.run();
            return self();
        }

        public B setMetadata(E metadata) {
            this.metadata = metadata;
            return self();
        }

        public FidgetzButton<E> build() {
            return new FidgetzButton<>(this);
        }
    }
}
