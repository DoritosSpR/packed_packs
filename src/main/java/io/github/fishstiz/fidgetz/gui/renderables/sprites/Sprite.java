package io.github.fishstiz.fidgetz.gui.renderables.sprites;

import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.shapes.Line;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class Sprite implements RenderableRect {
    public final ResourceLocation location;
    public final int width;
    public final int height;
    public final Line u;
    public final Line v;

    public Sprite(ResourceLocation location, int width, int height, Line u, Line v) {
        this.location = location;
        this.width = width;
        this.height = height;
        this.u = u;
        this.v = v;
    }

    public Sprite(ResourceLocation location, int width, int height) {
        this(location, width, height, new Line(0, width), new Line(0, height));
    }

    public Sprite(ResourceLocation location, Size size, Line u, Line v) {
        this(location, size.width(), size.height(), u, v);
    }

    public Sprite(ResourceLocation location, Size size) {
        this(location, size.width(), size.height());
    }

    public static Sprite of32(ResourceLocation location) {
        return new Sprite(location, Size.of32());
    }

    public static Sprite of16(ResourceLocation location) {
        return new Sprite(location, Size.of16());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        guiGraphics.blit(
                RenderType::guiTextured,
                this.location,
                x, y,
                this.u.start(), this.v.start(),
                width, height,
                this.u.length(), this.v.length(),
                this.width, this.height
        );
    }

    public void render(GuiGraphics guiGraphics, int x, int y) {
        this.render(guiGraphics, x, y, this.width, this.height, 0);
    }

    public void renderClamped(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        int drawWidth = Math.min(width, this.width);
        int drawHeight = Math.min(height, this.height);

        int offsetX = (width - drawWidth) / 2;
        int offsetY = (height - drawHeight) / 2;

        this.render(guiGraphics, x + offsetX, y + offsetY, drawWidth, drawHeight, partialTick);
    }
}
