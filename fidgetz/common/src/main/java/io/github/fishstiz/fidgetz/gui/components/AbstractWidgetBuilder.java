package io.github.fishstiz.fidgetz.gui.components;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractWidgetBuilder<B> {
    public static final int DEFAULT_WIDTH = 150;
    public static final int DEFAULT_HEIGHT = 20;
    protected int x;
    protected int y;
    protected int width = DEFAULT_WIDTH;
    protected int height = DEFAULT_HEIGHT;

    @SuppressWarnings("unchecked")
    protected B self() {
        return (B) this;
    }

    public B setX(int x) {
        this.x = x;
        return this.self();
    }

    public B setY(int y) {
        this.y = y;
        return this.self();
    }

    public B setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        return this.self();
    }

    public B setWidth(int width) {
        this.width = width;
        return this.self();
    }

    public B setHeight(int height) {
        this.height = height;
        return this.self();
    }

    public B setDimensions(int width, int height) {
        this.setWidth(width);
        this.setHeight(height);
        return this.self();
    }

    public B makeSquare(int size) {
        return this.setDimensions(size, size);
    }

    public B makeSquare() {
        return this.makeSquare(this.height);
    }
}
