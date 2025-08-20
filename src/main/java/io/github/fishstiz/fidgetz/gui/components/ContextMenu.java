package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.gui.WidgetBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.fidgetz.util.ITheme;
import io.github.fishstiz.packed_packs.util.InputUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ContextMenu extends ToggleableDialog<LayoutWrapper<LinearLayout>> {
    public static final int DEFAULT_BACKGROUND_COLOR = Theme.GRAY_800.getARGB();
    public static final int DEFAULT_BORDER_COLOR = Theme.GRAY_500.getARGB();
    private final int borderColor;

    protected ContextMenu(Builder builder) {
        super(builder);

        this.borderColor = builder.borderColor;
    }

    private void setOptions(Option... options) {
        this.clearWidgets();
        this.root().setLayout(emptyLayout());
        for (Option option : options) {
            OptionWidget optionWidget = new OptionWidget(this.root().getWidth(), this.borderColor, option);
            this.addRenderableWidget(this.root().layout().addChild(optionWidget));
        }
    }

    public void open(double mouseX, double mouseY, Option... options) {
        if (options.length == 0) return;

        this.setOptions(options);
        this.root().arrangeElements();

        int menuWidth = this.root().layout().getWidth();
        int menuHeight = this.root().layout().getHeight();
        int menuX = (int) mouseX;
        int menuY = (int) mouseY;
        if (menuX + menuWidth > this.screen.width) {
            menuX = Math.max(0, this.screen.width - menuWidth);
        }
        if (menuY + menuHeight > this.screen.height) {
            menuY = Math.max(0, this.screen.height - menuHeight);
        }

        this.root().setPosition(menuX, menuY);
        this.setOpen(true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean clicked = super.mouseClicked(mouseX, mouseY, button);
        if (InputUtil.isLeftClick(button)) this.setOpen(false);
        return clicked;
    }

    private static LinearLayout emptyLayout() {
        return LinearLayout.vertical();
    }

    public static <S extends Screen & ToggleableDialogContainer> ContextMenu.Builder builder(S screen) {
        return new ContextMenu.Builder(screen, new LayoutWrapper<>(emptyLayout(), WidgetBuilder.DEFAULT_WIDTH, 0));
    }

    public static class Builder extends ToggleableDialog.Builder<LayoutWrapper<LinearLayout>, Builder> {
        protected int backgroundColor = DEFAULT_BACKGROUND_COLOR;
        protected int borderColor = DEFAULT_BORDER_COLOR;

        protected <S extends Screen & ToggleableDialogContainer> Builder(S screen, LayoutWrapper<LinearLayout> root) {
            super(screen, root);
        }

        public Builder setBorderColor(int borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        @Override
        public Builder setBackground(int color) {
            this.backgroundColor = color;
            return this;
        }

        @Override
        public ContextMenu build() {
            if (this.background == null) {
                this.background = new MenuBackground(this.backgroundColor, this.borderColor);
            }

            return new ContextMenu(this);
        }
    }

    public interface Option {
        Component text();

        Runnable onPress();
    }

    public record SimpleOption(Component text, Runnable onPress) implements Option {
    }

    private record MenuBackground(int backgroundColor, int borderColor) implements RenderableRect {
        @Override
        public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
            guiGraphics.fill(x, y, x + width, y + height, this.backgroundColor);
            guiGraphics.renderOutline(x, y, width, height, this.borderColor);
        }
    }

    private static class OptionWidget extends AbstractWidget {
        private static final int HOVER_OVERLAY_COLOR = Theme.WHITE.withAlpha(0.10f);
        private static final int DEFAULT_HEIGHT = 16;
        private static final int SPACING = GuiConstants.SPACING / 2;
        private final StringWidget text;
        private final Runnable onPress;
        private final int separatorColor;

        OptionWidget(int width, int borderColor, ContextMenu.Option option) {
            super(0, 0, width, DEFAULT_HEIGHT, option.text());

            this.text = FidgetzText.<Void>builder().setMessage(option.text()).alignLeft().build();
            this.separatorColor = ITheme.withAlpha(borderColor, 0.5f);
            this.onPress = option.onPress();
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int right = GuiUtil.getRight(this);
            int bottom = GuiUtil.getBottom(this);
            int innerW = this.getWidth() - SPACING * 2;
            int innerH = this.getHeight() - SPACING * 2;

            if (this.isHovered()) {
                guiGraphics.fill(x, y, right, bottom, HOVER_OVERLAY_COLOR);
            }
            guiGraphics.hLine(x, right - 1, bottom - 1, this.separatorColor);

            this.text.setPosition(x + SPACING, y + SPACING);
            this.text.setSize(innerW, innerH);
            this.text.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            narrationElementOutput.add(NarratedElementType.TITLE, this.getMessage());
        }
    }
}
