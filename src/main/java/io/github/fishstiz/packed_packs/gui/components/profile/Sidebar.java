package io.github.fishstiz.packed_packs.gui.components.profile;

import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import static io.github.fishstiz.fidgetz.util.DrawUtil.DEMO_BACKGROUND;

public class Sidebar extends ToggleableDialog<LayoutWrapper<FlexLayout>> implements ContextMenuContainer {
    private static final int MIN_WIDTH = 100;
    private final FidgetzButton<Void> closeButton;

    protected Sidebar(Builder builder) {
        super(builder);

        this.root().setPadding(GuiConstants.SPACING);
        this.root().setMessage(builder.title);
        this.root().setMinWidth(builder.minWidth);

        this.closeButton = FidgetzButton.<Void>builder()
                .makeSquare()
                .setMessage(CommonComponents.GUI_DONE)
                .setSprite(GuiConstants.CROSS_SPRITE)
                .setOnPress(() -> this.setOpen(false))
                .build();
        final FidgetzText<Void> title = FidgetzText.<Void>builder()
                .setMessage(builder.title)
                .setShadow(builder.shadow)
                .setOffsetY(1)
                .alignLeft()
                .build();

        final int maxWidth = Math.max(builder.minWidth, builder.maxWidth);
        final FlexLayout header = FlexLayout.horizontal(() -> maxWidth).spacing(GuiConstants.SPACING);
        header.addChild(this.closeButton);
        header.addFlexChild(title);

        this.root().layout().addChild(header);
    }

    public FidgetzButton<Void> getCloseButton() {
        return this.closeButton;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.isMouseOverBounds(mouseX, mouseY);
    }

    public void repositionElements() {
        this.root().setMinHeight(getMaxHeight(this.screen));
        this.root().arrangeElements();
        this.root().setPosition(0, 0);
    }

    private static int getMaxHeight(Screen screen) {
        return screen.height - GuiConstants.SPACING * 2;
    }

    public static <S extends Screen & ToggleableDialogContainer> Builder builder(S screen) {
        return new Builder(screen, new LayoutWrapper<>(FlexLayout.vertical(() -> getMaxHeight(screen)).spacing(GuiConstants.SPACING)));
    }

    public static class Builder extends ToggleableDialog.Builder<LayoutWrapper<FlexLayout>, Builder> {
        private Component title;
        private boolean shadow;
        private int minWidth = MIN_WIDTH;
        private int maxWidth;

        protected <S extends Screen & ToggleableDialogContainer> Builder(S screen, LayoutWrapper<FlexLayout> root) {
            super(screen, root);
            this.background = DEMO_BACKGROUND;
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
            return new Sidebar(this);
        }
    }
}
