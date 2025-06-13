package io.github.fishstiz.fidgetz.gui.renderables.sprites;

import io.github.fishstiz.fidgetz.gui.shapes.Line;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

public class RepeatingSprite extends Sprite {
    public RepeatingSprite(ResourceLocation location, int textureWidth, int textureHeight, Line u, Line v) {
        super(location, textureWidth, textureHeight, u, v);
    }

    public RepeatingSprite(ResourceLocation location, int textureWidth, int textureHeight) {
        super(location, textureWidth, textureHeight);
    }

    public RepeatingSprite(ResourceLocation location, Size size, Line u, Line v) {
        this(location, size.width(), size.height(), u, v);
    }

    public RepeatingSprite(ResourceLocation location, Size size) {
        this(location, size.width(), size.height());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        for (int drawX = 0; drawX < width; drawX += this.u.length()) {
            for (int drawY = 0; drawY < height; drawY += this.v.length()) {
                int tileWidth = Math.min(this.u.length(), width - drawX);
                int tileHeight = Math.min(this.v.length(), height - drawY);

                guiGraphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        this.location,
                        x + drawX, y + drawY,
                        this.u.start(), this.v.start(),
                        tileWidth, tileHeight,
                        tileWidth, tileHeight,
                        this.width, this.height
                );
            }
        }
    }
}
