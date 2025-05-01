package io.github.fishstiz.fidgetz.gui.layouts;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class FlexLayout implements Layout {
    private final List<Child<? extends LayoutElement>> children = new ArrayList<>();
    private final LinearLayout wrappedLayout;
    private final LinearLayout.Orientation orientation;
    private final IntSupplier maxSize;
    private int maxWidth;
    private int maxHeight;
    private int spacing;

    protected FlexLayout(LinearLayout.Orientation orientation, IntSupplier maxSize, int maxWidth, int maxHeight, int spacing) {
        this.wrappedLayout = new LinearLayout(0, 0, orientation).spacing(spacing);
        this.orientation = orientation;
        this.maxSize = maxSize;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.spacing = spacing;
    }

    public <T extends LayoutElement> T addChild(T child) {
        this.children.add(new Child<>(child));
        return this.wrappedLayout.addChild(child);
    }

    public <T extends AbstractWidget> T addFlexChild(T child, boolean crossAxis) {
        this.children.add(new FlexWidget(child, crossAxis));
        return this.wrappedLayout.addChild(child);
    }

    public <T extends FlexLayout> T addFlexChild(T child, boolean crossAxis) {
        this.children.add(new NestedFlexLayout(child, crossAxis));
        return this.wrappedLayout.addChild(child);
    }

    private int getFlexDistribution() {
        int totalSize = 0;
        int flexCount = 0;

        for (int i = 0; i < this.children.size(); i++) {
            Child<?> child = this.children.get(i);

            if (child instanceof FlexChild<?>) {
                flexCount++;
            } else {
                totalSize += child.getAxisSize(this.orientation);
            }

            if (i < this.children.size() - 1) {
                totalSize += spacing;
            }
        }

        return flexCount > 0 ? (this.maxSize.getAsInt() - totalSize) / flexCount : 0;
    }

    @Override
    public void arrangeElements() {
        int distribution = this.getFlexDistribution();

        for (Child<?> child : this.children) {
            if (child instanceof FlexChild<?> flexChild) {
                flexChild.setAxisSizes(this.orientation, distribution, this.maxWidth, this.maxHeight);
            }
        }

        this.wrappedLayout.arrangeElements();
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        this.wrappedLayout.visitChildren(visitor);
    }

    @Override
    public void setX(int x) {
        this.wrappedLayout.setX(x);
    }

    @Override
    public void setY(int y) {
        this.wrappedLayout.setY(y);
    }

    @Override
    public int getX() {
        return this.wrappedLayout.getX();
    }

    @Override
    public int getY() {
        return this.wrappedLayout.getY();
    }

    @Override
    public int getWidth() {
        return this.wrappedLayout.getWidth();
    }

    @Override
    public int getHeight() {
        return this.wrappedLayout.getHeight();
    }

    public FlexLayout spacing(int spacing) {
        this.wrappedLayout.spacing(spacing);
        this.spacing = spacing;
        return this;
    }

    public static FlexLayout horizontal(IntSupplier maxWidth) {
        return new FlexLayout(LinearLayout.Orientation.HORIZONTAL, maxWidth, maxWidth.getAsInt(), 0, 0);
    }

    public static FlexLayout vertical(IntSupplier maxHeight) {
        return new FlexLayout(LinearLayout.Orientation.VERTICAL, maxHeight, 0, maxHeight.getAsInt(), 0);
    }

    private static class Child<T extends LayoutElement> {
        protected final T element;

        protected Child(T element) {
            this.element = element;
        }

        protected int getAxisSize(LinearLayout.Orientation orientation) {
            return orientation == LinearLayout.Orientation.HORIZONTAL ? this.getWidth() : this.getHeight();
        }

        protected int getWidth() {
            return this.element.getWidth();
        }

        protected int getHeight() {
            return this.element.getHeight();
        }
    }

    private abstract static class FlexChild<T extends LayoutElement> extends Child<T> {
        protected final boolean crossAxis;

        protected FlexChild(T element, boolean crossAxis) {
            super(element);
            this.crossAxis = crossAxis;
        }

        protected abstract void setWidth(int width);

        protected abstract void setHeight(int height);

        protected void setAxisSizes(LinearLayout.Orientation orientation, int size, int maxWidth, int maxHeight) {
            if (orientation == LinearLayout.Orientation.HORIZONTAL) {
                this.setWidth(size);
                if (this.crossAxis && maxHeight > 0) {
                    this.setHeight(maxHeight);
                }
            } else {
                this.setHeight(size);
                if (this.crossAxis && maxWidth > 0) {
                    this.setWidth(maxWidth);
                }
            }
        }
    }

    private static class FlexWidget extends FlexChild<AbstractWidget> {
        private FlexWidget(AbstractWidget element, boolean crossAxis) {
            super(element, crossAxis);
        }

        @Override
        protected void setWidth(int width) {
            this.element.setWidth(width);
        }

        @Override
        protected void setHeight(int height) {
            this.element.setHeight(height);
        }
    }

    private static class NestedFlexLayout extends FlexChild<FlexLayout> {
        protected NestedFlexLayout(FlexLayout element, boolean crossAxis) {
            super(element, crossAxis);
        }

        @Override
        protected int getWidth() {
            return this.element.maxWidth;
        }

        @Override
        protected int getHeight() {
            return this.element.maxHeight;
        }

        @Override
        protected void setWidth(int width) {
            this.element.maxWidth = width;
        }

        @Override
        protected void setHeight(int height) {
            this.element.maxHeight = height;
        }
    }
}
