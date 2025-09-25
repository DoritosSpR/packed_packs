package io.github.fishstiz.fidgetz.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class LayoutWrapper<T extends Layout> extends AbstractWidget implements Layout {
    private T layout;
    private int minWidth;
    private int minHeight;

    public LayoutWrapper(T layout, int minWidth, int minHeight) {
        super(layout.getX(), layout.getY(), layout.getWidth(), layout.getHeight(), Component.empty());

        this.layout = layout;
        this.minWidth = minWidth;
        this.minHeight = minHeight;

        this.active = false;

        this.arrangeElements();
    }

    public LayoutWrapper(T layout) {
        this(layout, 0, 0);
    }

    public void setLayout(@NotNull T layout) {
        this.layout = layout;
        this.arrangeElements();
    }

    public T layout() {
        return this.layout;
    }

    @Override
    public void setWidth(int width) {
        this.width = Math.max(this.minWidth, width);
    }

    @Override
    public void setHeight(int height) {
        this.height = Math.max(this.minHeight, height);
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
        this.setWidth(this.width);
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
        this.setHeight(this.height);
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.layout.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.layout.setY(y);
    }

    @Override
    public int getX() {
        return this.layout.getX();
    }

    @Override
    public int getY() {
        return this.layout.getY();
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        this.layout.visitChildren(visitor);
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> consumer) {
        this.layout.visitWidgets(consumer);
    }

    @Override
    public void arrangeElements() {
        this.layout.arrangeElements();
        this.setPosition(this.layout.getX(), this.layout.getY());
        this.setWidth(this.layout.getWidth());
        this.setHeight(this.layout.getHeight());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // unsupported operation
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // unsupported operation
    }

    @Override
    public void playDownSound(SoundManager handler) {
        // unsupported operation
    }
}

