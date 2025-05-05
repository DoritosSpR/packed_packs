package io.github.fishstiz.fidgetz.gui.layouts;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class FlexLayout implements Layout {
    private final List<Child<? extends LayoutElement>> children = new ArrayList<>();
    private final LinearLayout wrappedLayout;
    private final LinearLayout.Orientation orientation;
    private final @Nullable IntSupplier maxSize;
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

    public <T extends LayoutElement> T addChild(T child, LayoutSettings layoutSettings) {
        this.children.add(new Child<>(child, layoutSettings));
        return this.wrappedLayout.addChild(child, layoutSettings);
    }

    public <T extends LayoutElement> T addChild(T child) {
        return this.addChild(child, this.wrappedLayout.newCellSettings());
    }

    public <T extends AbstractWidget> T addFlexChild(T child, boolean crossAxis, LayoutSettings layoutSettings) {
        this.children.add(new FlexWidget(child, crossAxis, layoutSettings));
        return this.wrappedLayout.addChild(child, layoutSettings);
    }

    public <T extends AbstractWidget> T addFlexChild(T child, boolean crossAxis) {
        return this.addFlexChild(child, crossAxis, this.wrappedLayout.newCellSettings());
    }

    public <T extends AbstractWidget> T addFlexChild(T child) {
        return this.addFlexChild(child, false, this.wrappedLayout.newCellSettings());
    }

    public <T extends FlexLayout> T addFlexChild(T child, boolean crossAxis, LayoutSettings layoutSettings) {
        this.children.add(new NestedFlexLayout(child, crossAxis, layoutSettings));
        return this.wrappedLayout.addChild(child, layoutSettings);
    }

    public <T extends FlexLayout> T addFlexChild(T child, boolean crossAxis) {
        return this.addFlexChild(child, crossAxis, this.wrappedLayout.newCellSettings());
    }

    public <T extends FlexLayout> T addFlexChild(T child) {
        return this.addFlexChild(child, false, this.wrappedLayout.newCellSettings());
    }

    private int getCurrentMax() {
        return this.orientation == LinearLayout.Orientation.HORIZONTAL ? this.maxWidth : this.maxHeight;
    }

    private int getFlexDistribution() {
        int padding = 0;
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

            var layoutSettings = child.layoutSettings.getExposed();
            padding += this.orientation == LinearLayout.Orientation.HORIZONTAL
                    ? layoutSettings.paddingLeft + layoutSettings.paddingTop
                    : layoutSettings.paddingTop + layoutSettings.paddingBottom;
        }

        int max = this.maxSize != null ? this.maxSize.getAsInt() : this.getCurrentMax();
        return flexCount > 0 ? (max - padding - totalSize) / flexCount : 0;
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

    public FlexLayout copyLayout() {
        return new FlexLayout(this.orientation, this.maxSize, this.maxWidth, this.maxHeight, this.spacing);
    }

    public static FlexLayout horizontal(IntSupplier maxWidth) {
        return new FlexLayout(LinearLayout.Orientation.HORIZONTAL, maxWidth, maxWidth.getAsInt(), 0, 0);
    }

    public static FlexLayout horizontal() {
        return new FlexLayout(LinearLayout.Orientation.HORIZONTAL, null, 0, 0, 0);
    }

    public static FlexLayout vertical(IntSupplier maxHeight) {
        return new FlexLayout(LinearLayout.Orientation.VERTICAL, maxHeight, 0, maxHeight.getAsInt(), 0);
    }

    public static FlexLayout vertical() {
        return new FlexLayout(LinearLayout.Orientation.VERTICAL, null, 0, 0, 0);
    }

    private static class Child<T extends LayoutElement> {
        protected final T element;
        protected final LayoutSettings layoutSettings;

        protected Child(T element, LayoutSettings layoutSettings) {
            this.element = element;
            this.layoutSettings = layoutSettings;
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

        protected FlexChild(T element, boolean crossAxis, LayoutSettings layoutSettings) {
            super(element, layoutSettings);

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
        private FlexWidget(AbstractWidget element, boolean crossAxis, LayoutSettings layoutSettings) {
            super(element, crossAxis, layoutSettings);
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
        protected NestedFlexLayout(FlexLayout element, boolean crossAxis, LayoutSettings layoutSettings) {
            super(element, crossAxis, layoutSettings);
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
