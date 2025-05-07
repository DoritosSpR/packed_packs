package io.github.fishstiz.packed_packs.gui.components.profile;

import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.shapes.Line;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Sidebar extends ToggleableDialog<LayoutWrapper<FlexLayout>> {
    private static final int SPACING = 8;
    private static final Sprite DEFAULT_BACKGROUND = new Sprite(
            ResourceLocation.withDefaultNamespace("textures/gui/demo_background.png"),
            Size.square(256),
            Line.zero(247),
            Line.zero(165)
    );
    private static final Sprite CLOSE_SPRITE = new Sprite(ResourceUtil.getIcon("cross"), Size.of16());
    private static final int MIN_WIDTH = 100;
    private final FidgetzButton<Void> closeButton;

    protected Sidebar(Builder builder) {
        super(builder);

        this.root().setMessage(builder.title);
        this.root().setMinWidth(builder.minWidth);

        int maxWidth = Math.max(builder.minWidth, builder.maxWidth);
        FlexLayout header = this.root().layout().addChild(FlexLayout.horizontal(() -> maxWidth));

        this.closeButton = header.addChild(
                FidgetzButton.<Void>builder()
                        .makeSquare()
                        .setMessage(CommonComponents.GUI_DONE)
                        .setSpriteOnly(CLOSE_SPRITE)
                        .setOnPress(() -> this.setOpen(false))
                        .build(),
                builder.headerSettings
        );

        Font font = Minecraft.getInstance().font;
        int titleFontWidth = font.width(builder.title);
        int titleWidth = MIN_WIDTH > titleFontWidth ? MIN_WIDTH : titleFontWidth + SPACING;
        header.addFlexChild(
                FidgetzText.builder(font)
                        .setDimensions(titleWidth, this.closeButton.getHeight())
                        .setMessage(builder.title)
                        .setShadow(builder.shadow)
                        .setOffsetY(1)
                        .alignLeft()
                        .build(),
                false,
                builder.headerSettings
        );

        header.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    public FidgetzButton<Void> getCloseButton() {
        return this.closeButton;
    }

    public void repositionElements() {
        this.root().setMinHeight(this.screen.height);
        this.root().arrangeElements();
    }

    public static <S extends Screen & ToggleableDialogContainer> Builder builder(S screen) {
        return new Builder(screen, new LayoutWrapper<>(
                FlexLayout.vertical(() -> screen.height).spacing(SPACING),
                MIN_WIDTH,
                Minecraft.getInstance().getWindow().getHeight()
        ));
    }

    public static class Builder extends ToggleableDialog.Builder<LayoutWrapper<FlexLayout>, Builder> {
        private Component title;
        private boolean shadow;
        private LayoutSettings headerSettings = LayoutSettings.defaults();
        private int minWidth = MIN_WIDTH;
        private int maxWidth;

        protected <S extends Screen & ToggleableDialogContainer> Builder(S screen, LayoutWrapper<FlexLayout> root) {
            super(screen, root);
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

        public Builder setMaxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
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
