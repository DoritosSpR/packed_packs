package io.github.fishstiz.fidgetz.gui.renderables.sprites;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class GuiSprite extends Sprite {
    public GuiSprite(ResourceLocation location, int width, int height) {
        super(location, width, height);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        guiGraphics.blitSprite(this.location, x, y, width, height);
    }
}
