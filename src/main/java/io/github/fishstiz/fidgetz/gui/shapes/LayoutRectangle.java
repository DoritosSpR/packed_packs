package io.github.fishstiz.fidgetz.gui.shapes;

import net.minecraft.client.gui.layouts.LayoutElement;

public interface LayoutRectangle {
    static LayoutRectangle viewOf(LayoutElement element) {
        return new LayoutRectangle() {
            private final LayoutElement layoutElement = element;

            @Override
            public int getX() {
                return this.layoutElement.getX();
            }

            @Override
            public int getY() {
                return this.layoutElement.getY();
            }

            @Override
            public int getWidth() {
                return this.layoutElement.getWidth();
            }

            @Override
            public int getHeight() {
                return this.layoutElement.getHeight();
            }
        };
    }

    int getX();

    int getY();

    int getWidth();

    int getHeight();

    default int getRight() {
        return this.getX() + this.getWidth();
    }

    default int getBottom() {
        return this.getY() + this.getHeight();
    }

    default boolean contains(int px, int py) {
        return px >= this.getX() && px < this.getRight() &&
               py >= this.getY() && py < this.getBottom();
    }

    default boolean contains(double px, double py) {
        return this.contains((int) px, (int) py);
    }

    default boolean contains(LayoutElement element) {
        return element.getX() >= this.getX() &&
               element.getY() >= this.getY() &&
               getRight(element) <= this.getRight() &&
               getBottom(element) <= this.getBottom();
    }

    default boolean intersects(LayoutElement element) {
        return this.getX() < getRight(element) &&
               this.getRight() > element.getX() &&
               this.getY() < getBottom(element) &&
               this.getBottom() > element.getY();
    }

    static int getRight(LayoutElement element) {
        return element.getX() + element.getHeight();
    }

    static int getBottom(LayoutElement element) {
        return element.getY() + element.getHeight();
    }
}
