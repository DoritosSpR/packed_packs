package io.github.fishstiz.fidgetz.gui.renderables.sprites;

import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Function;

public record ButtonSprites(Sprite active, Sprite inactive, Function<Sprite, RenderableRect> spriteRenderer) {
    public ButtonSprites(Sprite active, Sprite inactive) {
        this(active, inactive, sprite -> sprite::renderClamped);
    }

    public static ButtonSprites unclamp(Sprite active, Sprite inactive) {
        return new ButtonSprites(active, inactive, sprite -> sprite);
    }

    public static ButtonSprites unclamp(Sprite sprite) {
        return unclamp(sprite, sprite);
    }

    public static ButtonSprites of(Sprite sprite) {
        return new ButtonSprites(sprite, sprite);
    }

    public Sprite get(boolean active) {
        return active ? this.active : this.inactive;
    }

    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean active, float partialTick) {
        this.spriteRenderer.apply(this.get(active)).render(guiGraphics, x, y, width, height, partialTick);
    }
}
