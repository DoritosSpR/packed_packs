package io.github.fishstiz.fidgetz.gui.shapes;

import io.github.fishstiz.fidgetz.util.LogUtil;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LayoutElement;

import java.util.function.Consumer;

public final class GuiRectangle {
    private final LayoutElement widget;

    public GuiRectangle(int x, int y, int width, int height) {
        this.widget = new LayoutElement() {
            @Override
            public void setX(int x) {
                LogUtil.logUnsupported();
            }

            @Override
            public void setY(int y) {
                LogUtil.logUnsupported();
            }

            @Override
            public int getX() {
                return x;
            }

            @Override
            public int getY() {
                return y;
            }

            @Override
            public int getWidth() {
                return width;
            }

            @Override
            public int getHeight() {
                return height;
            }

            @Override
            public void visitWidgets(Consumer<AbstractWidget> consumer) {
                LogUtil.logUnsupported();
            }
        };
    }

    public <T extends LayoutElement> GuiRectangle(T widget) {
        this.widget = widget;
    }

    public int getX() {
        return this.widget.getX();
    }

    public int getY() {
        return this.widget.getY();
    }

    public int getWidth() {
        return this.widget.getWidth();
    }

    public int getHeight() {
        return this.widget.getHeight();
    }

    public int getRight() {
        return this.getX() + this.getY();
    }

    public int getBottom() {
        return this.getY() + this.getHeight();
    }

    public LayoutElement asWidget() {
        return this.widget;
    }
}
