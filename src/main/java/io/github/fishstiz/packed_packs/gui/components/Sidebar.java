package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.shapes.Line;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Sidebar extends ToggleableDialog<LayoutWrapper<LinearLayout>> {
    private static final Sprite DEFAULT_BACKGROUND = new Sprite(
            ResourceLocation.withDefaultNamespace("textures/gui/demo_background.png"),
            Size.square(256),
            Line.zero(247),
            Line.zero(165)
    );
    private static final int HEADER_SIZE = 20;
    private static final int MIN_WIDTH = 100;

    protected Sidebar(Builder builder) {
        super(builder);

        this.getRoot().setMessage(builder.title);
        this.getRoot().setMinWidth(builder.minWidth);

        LinearLayout header = this.getRoot().getLayout().addChild(LinearLayout.horizontal());

        header.addChild(FidgetzButton.builder()
                .setOnPress(() -> this.setOpen(false))
                .setDimensions(HEADER_SIZE, HEADER_SIZE)
                .build(), builder.headerSettings);

        Font font = Minecraft.getInstance().font;
        header.addChild(FidgetzText.builder(font)
                .setDimensions(Math.max(font.width(builder.title), MIN_WIDTH), HEADER_SIZE)
                .setMessage(builder.title)
                .setShadow(builder.shadow)
                .setOffsetY(1)
                .alignLeft()
                .build(), builder.headerSettings);

        header.visitWidgets(this::addRenderableWidget);
    }

    public void repositionElements() {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            this.getRoot().setMinHeight(screen.height);
            this.getRoot().repositionElements();
        }
    }

    public static Builder builder() {
        return new Builder(new LayoutWrapper<>(LinearLayout.vertical(), MIN_WIDTH, Minecraft.getInstance().getWindow().getHeight()));
    }

    public static class Builder extends ToggleableDialog.Builder<LayoutWrapper<LinearLayout>, Builder> {
        private Component title;
        private boolean shadow;
        private LayoutSettings headerSettings = LayoutSettings.defaults();
        private int minWidth = MIN_WIDTH;

        protected Builder(LayoutWrapper<LinearLayout> root) {
            super(root);
        }

        public Builder setHeaderSettings(LayoutSettings headerSettings) {
            this.headerSettings = headerSettings;
            return this;
        }

        public Builder setTitle(Component title) {
            this.title = title;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = Component.literal(title);
            return this;
        }

        public Builder setTitle(Component title, boolean shadow) {
            this.shadow = shadow;
            return this.setTitle(title);
        }

        public Builder setTitle(String title, boolean shadow) {
            this.shadow = shadow;
            return this.setTitle(Component.literal(title));
        }

        public Builder setMinWidth(int minWidth) {
            this.minWidth = minWidth;
            return this;
        }

        @Override
        public Sidebar build() {
            if (this.background == null) {
                this.setBackground(DEFAULT_BACKGROUND);
            }

            return new Sidebar(this);
        }
    }
}
