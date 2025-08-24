package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import net.minecraft.client.gui.GuiGraphics;

public interface MenuItemRenderer extends RenderableRect {
    void render(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int spacing,
            boolean hovered,
            double mouseX,
            double mouseY,
            float partialTick
    );

    @Override
    default void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
    }
}
