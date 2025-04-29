package io.github.fishstiz.fidgetz.gui;

import io.github.fishstiz.fidgetz.gui.shapes.GuiRectangle;
import net.minecraft.client.gui.GuiGraphics;

public interface RenderableRectangle {
    void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float tick);

    default void render(GuiGraphics guiGraphics, GuiRectangle rect, float partialTick) {
        this.render(guiGraphics, rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), partialTick);
    }
}
