package io.github.fishstiz.fidgetz.gui.components;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * combined copy of AbstractScrollArea and AbstractContainerWidget from 1.21.4
 */
public abstract class AbstractScrollContainer extends AbstractWidget implements ContainerEventHandler {
    public static final int SCROLLBAR_WIDTH = 6;
    private double scrollAmount;
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller");
    private static final ResourceLocation SCROLLER_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller_background");
    private @Nullable GuiEventListener focused;
    private boolean dragging;
    private boolean scrolling;

    public AbstractScrollContainer(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public @Nullable GuiEventListener getFocused() {
        return this.focused;
    }

    @Override
    public boolean isFocused() {
        return ContainerEventHandler.super.isFocused();
    }

    @Override
    public void setFocused(boolean focused) {
        // nop
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        if (this.focused != null) {
            this.focused.setFocused(false);
        }

        if (focused != null) {
            focused.setFocused(true);
        }

        this.focused = focused;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return ContainerEventHandler.super.nextFocusPath(event);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.visible) {
            return false;
        } else {
            this.setScrollAmount(this.scrollAmount() - scrollY * this.scrollRate());
            return true;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isMouseOver(mouseX, mouseY)) return false;
        boolean scrolled = this.updateScrolling(mouseX, mouseY, button);
        return ContainerEventHandler.super.mouseClicked(mouseX, mouseY, button) || scrolled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        return ContainerEventHandler.super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling) {
            if (mouseY < (double) this.getY()) {
                this.setScrollAmount(0.0F);
            } else if (mouseY > (double) this.getBottom()) {
                this.setScrollAmount(this.maxScrollAmount());
            } else {
                double scrollRange = Math.max(1, this.maxScrollAmount());
                double scrollRatio = Math.max(1.0F, scrollRange / (double) (this.height - this.scrollerHeight()));
                this.setScrollAmount(this.scrollAmount() + dragY * scrollRatio);
            }
        } else {
            super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        return ContainerEventHandler.super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public void onRelease(double mouseX, double mouseY) {
        this.scrolling = false;
    }

    public double scrollAmount() {
        return this.scrollAmount;
    }

    public void setScrollAmount(double scrollAmount) {
        this.scrollAmount = Mth.clamp(scrollAmount, 0.0F, this.maxScrollAmount());
    }

    public boolean updateScrolling(double mouseX, double mouseY, int button) {
        this.scrolling = this.scrollbarVisible() && this.isValidClickButton(button) &&
                         mouseX >= (double) this.scrollBarX() &&
                         mouseX <= (double) (this.scrollBarX() + SCROLLBAR_WIDTH) &&
                         mouseY >= (double) this.getY() &&
                         mouseY < (double) this.getBottom();

        return this.scrolling;
    }

    public void refreshScrollAmount() {
        this.setScrollAmount(this.scrollAmount);
    }

    public int maxScrollAmount() {
        return Math.max(0, this.contentHeight() - this.height);
    }

    protected boolean scrollbarVisible() {
        return this.maxScrollAmount() > 0;
    }

    protected int scrollerHeight() {
        return Mth.clamp((int) ((float) (this.height * this.height) / (float) this.contentHeight()), 32, this.height - 8);
    }

    protected int scrollBarX() {
        return this.getRight() - SCROLLBAR_WIDTH;
    }

    protected int scrollBarY() {
        return Math.max(this.getY(), (int) this.scrollAmount * (this.height - this.scrollerHeight()) / this.maxScrollAmount() + this.getY());
    }

    protected void renderScrollbar(GuiGraphics guiGraphics) {
        if (this.scrollbarVisible()) {
            int scrollBarX = this.scrollBarX();
            int scrollBarY = this.scrollBarY();
            int scrollBarHeight = this.scrollerHeight();

            guiGraphics.blitSprite(SCROLLER_BACKGROUND_SPRITE, scrollBarX, this.getY(), SCROLLBAR_WIDTH, this.getHeight());
            guiGraphics.blitSprite(SCROLLER_SPRITE, scrollBarX, scrollBarY, SCROLLBAR_WIDTH, scrollBarHeight);
        }
    }

    protected abstract int contentHeight();

    protected abstract double scrollRate();
}
