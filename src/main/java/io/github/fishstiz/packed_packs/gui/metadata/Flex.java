package io.github.fishstiz.packed_packs.gui.metadata;

import io.github.fishstiz.fidgetz.gui.Metadata;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LayoutElement;

import java.util.*;
import java.util.function.Supplier;

public record Flex(Orientation orientation, Supplier<Integer> maxSupplier, int gap, LayoutElement... siblings) {
    public Flex {
        Objects.requireNonNull(orientation);
        Objects.requireNonNull(maxSupplier);
    }

    public boolean isHorizontal() {
        return this.orientation() == Orientation.HORIZONTAL;
    }

    public boolean isVertical() {
        return this.orientation == Orientation.VERTICAL;
    }

    public int getSize(boolean excludeFlexWidgets) {
        int size = 0;

        for (var sibling : this.siblings) {
            boolean isFlexWidget = excludeFlexWidgets
                                   && sibling instanceof AbstractWidget
                                   && sibling instanceof Metadata<?> metadata
                                   && metadata.getMetadata() instanceof Flex;

            if (!isFlexWidget) size += (orientation == Orientation.HORIZONTAL) ? sibling.getWidth() : sibling.getHeight();
            size += this.gap;
        }

        return this.maxSupplier.get() - size;
    }

    public int getSize() {
        return this.getSize(false);
    }

    public static Flex horizontal(Supplier<Integer> maxWidthSupplier, int gap, LayoutElement... siblings) {
        return new Flex(Orientation.HORIZONTAL, maxWidthSupplier, gap, siblings);
    }

    public static Flex vertical(Supplier<Integer> maxHeightSupplier, int gap, LayoutElement... siblings) {
        return new Flex(Orientation.VERTICAL, maxHeightSupplier, gap, siblings);
    }

    private record Parent(List<AbstractWidget> flexChildren, Flex flex) {
    }
    
    public static <T extends LayoutElement> void applyFlex(T element) {
        final List<Parent> parents = new ArrayList<>();

        element.visitWidgets(widget -> {
            if (widget instanceof Metadata<?> meta && meta.getMetadata() instanceof Flex flex) {
                for (var parent : parents) {
                    if (parent.flexChildren().contains(widget)) {
                        return;
                    }
                }
                List<AbstractWidget> children = new ArrayList<>();
                children.add(widget);
                children.addAll(Arrays.stream(flex.siblings())
                        .filter(sibling -> sibling instanceof AbstractWidget
                                           && sibling instanceof Metadata<?> metadata
                                           && metadata.getMetadata() instanceof Flex)
                        .map(AbstractWidget.class::cast)
                        .toList());
                parents.add(new Parent(children, flex));
            }
        });

        for (var parent : parents) {
            int distributedSize = parent.flex().getSize(true) / parent.flexChildren().size();
            for (var flexChild : parent.flexChildren()) {
                if (parent.flex().isHorizontal()) {
                    flexChild.setWidth(distributedSize);
                } else {
                    flexChild.setHeight(distributedSize);
                }
            }
        }
    }

    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }
}