package io.github.fishstiz.fidgetz.gui.layouts;

import io.github.fishstiz.fidgetz.gui.components.AbstractScrollContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A copy of ScrollableLayout from 1.21.6, but without the padding.
 */
public class ScrollableLayout implements Layout {
    private final Layout content;
    private final Container container;
    private int minWidth;
    private int maxHeight;

    public ScrollableLayout(Minecraft minecraft, Layout content, int height) {
        this.content = content;
        this.container = new Container(minecraft, 0, height);
    }

    public ScrollableLayout(Minecraft minecraft, Layout content) {
        this(minecraft, content, content.getHeight());
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
        this.container.setWidth(Math.max(this.content.getWidth(), minWidth));
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        this.container.setHeight(Math.min(this.content.getHeight(), maxHeight));
        this.container.refreshScrollAmount();
    }

    @Override
    public void arrangeElements() {
        this.content.arrangeElements();

        boolean scrollbarVisible = this.content.getHeight() > this.maxHeight;
        // removed padding (content width + 20)
        this.container.setWidth(Math.max(this.content.getWidth() + (scrollbarVisible ? AbstractScrollContainer.SCROLLBAR_WIDTH : 0), this.minWidth));
        this.container.setHeight(Math.min(this.content.getHeight(), this.maxHeight));
        this.container.refreshScrollAmount();
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        visitor.accept(this.container);
    }

    @Override
    public void setX(int x) {
        this.container.setX(x);
    }

    @Override
    public void setY(int y) {
        this.container.setY(y);
    }

    @Override
    public int getX() {
        return this.container.getX();
    }

    @Override
    public int getY() {
        return this.container.getY();
    }

    @Override
    public int getWidth() {
        return this.container.getWidth();
    }

    @Override
    public int getHeight() {
        return this.container.getHeight();
    }

    class Container extends AbstractScrollContainer {
        private static final double SCROLL_PADDING = 14.0;
        private static final int SCROLL_RATE = 10;
        private final Minecraft minecraft;
        private final List<AbstractWidget> children = new ArrayList<>();

        public Container(final Minecraft minecraft, final int width, final int height) {
            super(0, 0, width, height, CommonComponents.EMPTY);
            this.minecraft = minecraft;
            ScrollableLayout.this.content.visitWidgets(this.children::add);
        }

        @Override
        protected int contentHeight() {
            return ScrollableLayout.this.content.getHeight();
        }

        @Override
        protected double scrollRate() {
            return SCROLL_RATE;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);

            for (AbstractWidget abstractWidget : this.children) {
                abstractWidget.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            guiGraphics.disableScissor();
            this.renderScrollbar(guiGraphics);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }

        @Override
        public @NotNull ScreenRectangle getRectangle() {
            return new ScreenRectangle(this.getX(), this.getY(), this.width, this.contentHeight());
        }

        @Override
        public void setFocused(@Nullable GuiEventListener focused) {
            super.setFocused(focused);
            if (focused != null && this.minecraft.getLastInputType().isKeyboard()) {
                ScreenRectangle containerRectangle = this.getRectangle();
                ScreenRectangle focusedRectangle = focused.getRectangle();
                int relativeTop = focusedRectangle.top() - containerRectangle.top();
                int relativeBottom = focusedRectangle.bottom() - containerRectangle.bottom();
                if (relativeTop < 0) {
                    this.setScrollAmount(this.scrollAmount() + relativeTop - SCROLL_PADDING);
                } else if (relativeBottom > 0) {
                    this.setScrollAmount(this.scrollAmount() + relativeBottom + SCROLL_PADDING);
                }
            }
        }

        @Override
        public void setX(int x) {
            super.setX(x);
            ScrollableLayout.this.content.setX(x); // removed padding (x + 10)
        }

        @Override
        public void setY(int y) {
            super.setY(y);
            ScrollableLayout.this.content.setY(y - (int) this.scrollAmount());
        }

        @Override
        public void setScrollAmount(double scrollAmount) {
            super.setScrollAmount(scrollAmount);
            ScrollableLayout.this.content.setY(this.getRectangle().top() - (int) this.scrollAmount());
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return this.children;
        }
    }
}