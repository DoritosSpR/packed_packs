package io.github.fishstiz.fidgetz.gui.shapes;

public record Size(int width, int height) {
    public static Size square(int size) {
        return new Size(size, size);
    }

    public static Size of32() {
        return square(32);
    }
}
