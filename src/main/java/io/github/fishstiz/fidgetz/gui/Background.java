package io.github.fishstiz.fidgetz.gui;

import io.github.fishstiz.fidgetz.gui.sprites.Sprite;
import net.minecraft.client.gui.GuiGraphics;

public sealed interface Background permits Background.Color, Background.Texture, Background.Custom {
    record Color(int value) implements Background {
        public void render(GuiGraphics guiGraphics, int x, int y, int width, int height) {
            guiGraphics.fill(x, y, x + width, y + height, this.value);
        }
    }

    record Texture(Sprite sprite) implements Background {
    }

    record Custom(RenderableRectangle rectangle) implements Background {
    }
}
