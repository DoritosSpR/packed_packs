package io.github.fishstiz.fidgetz.gui;

import org.jetbrains.annotations.NotNull;

public interface WidgetBuilder<B> {
    int DEFAULT_WIDTH = 150;
    int DEFAULT_HEIGHT = 20;

    @NotNull B setX(int x);

    @NotNull B setY(int y);

    @NotNull B setPosition(int x, int y);

    @NotNull B setWidth(int width);

    @NotNull B setHeight(int height);

    @NotNull B setDimensions(int width, int height);
}
