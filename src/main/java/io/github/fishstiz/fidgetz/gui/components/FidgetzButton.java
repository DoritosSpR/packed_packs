package io.github.fishstiz.fidgetz.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.gui.Metadata;
import io.github.fishstiz.fidgetz.gui.WidgetBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuProvider;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class FidgetzButton<E> extends Button implements Fidgetz, ContextMenuProvider, Metadata<E> {
    private final List<Runnable> listeners = new ArrayList<>();
    private final ButtonSprites sprites;
    private final Integer focusedBorder;
    private final boolean spriteOnly;
    private final boolean focusOnInteract;
    private final BiConsumer<FidgetzButton<E>, ContextMenuItemBuilder> contextMenuBuilder;
    private final RenderableRect foreground;
    private E metadata;

    protected FidgetzButton(Builder<E, ?> builder) {
        super(builder.x, builder.y, builder.width, builder.height, builder.message, builder.onPress, DEFAULT_NARRATION);

        this.metadata = builder.metadata;
        this.sprites = builder.sprites;
        this.spriteOnly = builder.spriteOnly;
        this.foreground = builder.foreground;
        this.focusedBorder = builder.focusedBorder;
        this.focusOnInteract = builder.focusOnInteract;
        this.contextMenuBuilder = builder.contextMenuBuilder;

        if (builder.tooltip != null) {
            this.setTooltip(builder.tooltip);
        }
    }

    @Override
    public void onPress(InputWithModifiers inputWithModifiers) {
        super.onPress(inputWithModifiers);

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
        this.sprites.render(guiGraphics, x, y, width, height, this.active, partialTick);
    }

    protected void renderBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        DrawUtil.renderOutline(guiGraphics, x, y, width, height, this.focusedBorder);
    }

    protected void renderForeground(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        if (this.foreground != null) {
            this.foreground.render(guiGraphics, x, y, width, height, partialTick);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.isHovered = this.isHovered && Fidgetz.super.isMouseOver(mouseX, mouseY);

        if (!this.spriteOnly) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (this.hasSprite()) {
            int spriteWidth = this.getWidth();
            int spriteHeight = this.getHeight();
            int spriteX = this.getX() + (this.getWidth() - spriteWidth) / 2;
            int spriteY = this.getY() + (this.getHeight() - spriteHeight) / 2;

            this.renderSprite(guiGraphics, spriteX, spriteY, spriteWidth, spriteHeight, partialTick);
        }

        if (this.isHoveredOrFocused() && this.focusedBorder != null) {
            this.renderBorder(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), partialTick);
        }

        this.renderForeground(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), partialTick);

        this.updateCursor(guiGraphics);
    }

    protected void updateCursor(GuiGraphics guiGraphics) {
        if (this.isHovered()) {
            guiGraphics.requestCursor(this.isActive() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
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
        return super.isMouseOver(mouseX, mouseY) && Fidgetz.super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return this.focusOnInteract;
    }

    @Override
    public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
        if (this.contextMenuBuilder != null) {
            this.contextMenuBuilder.accept(this, builder);
        }
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
        private boolean spriteOnly = false;
        private RenderableRect foreground;
        private Integer focusedBorder;
        private boolean focusOnInteract = true;
        private OnPress onPress = btn -> {
        };
        private BiConsumer<FidgetzButton<E>, ContextMenuItemBuilder> contextMenuBuilder;
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

        public B setSprite(ButtonSprites sprites) {
            this.sprites = sprites;
            return self();
        }

        public B setSprite(Sprite sprite) {
            this.sprites = ButtonSprites.of(sprite);
            return self();
        }

        public B spriteOnly() {
            this.spriteOnly = true;
            return self();
        }

        public B setForeground(RenderableRect foreground) {
            this.foreground = foreground;
            return self();
        }

        public B setFocusedBorder(Integer hoverBorder) {
            this.focusedBorder = hoverBorder;
            return self();
        }

        public B setFocusOnInteract(boolean focusOnInteract) {
            this.focusOnInteract = focusOnInteract;
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

        public B setContextMenuBuilder(BiConsumer<FidgetzButton<E>, ContextMenuItemBuilder> contextMenuBuilder) {
            this.contextMenuBuilder = contextMenuBuilder;
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
