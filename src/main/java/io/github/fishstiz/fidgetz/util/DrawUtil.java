package io.github.fishstiz.fidgetz.util;

import io.github.fishstiz.fidgetz.gui.renderables.sprites.NineSliceSprite;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Line;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class DrawUtil {
    public static final Sprite DEMO_BACKGROUND = new NineSliceSprite(new Sprite(
            ResourceLocation.withDefaultNamespace("textures/gui/demo_background.png"),
            Size.square(256),
            Line.zero(247),
            Line.zero(165)
    ));

    private DrawUtil() {
    }

    public static void renderScrollingStringLeftAlign(
            GuiGraphics guiGraphics,
            Font font,
            Component text,
            int startX,
            int startY,
            int endX,
            int endY,
            int color,
            boolean shadow
    ) {
        int textWidth = font.width(text);
        int textY = (startY + endY - 9) / 2 + 1;
        int availableWidth = endX - startX;

        if (textWidth > availableWidth) {
            int overflowWidth = textWidth - availableWidth;
            double timeSeconds = Util.getMillis() / 1000.0;
            double scrollDuration = Math.max(overflowWidth * 0.5, 3.0);
            double scrollFactor = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * timeSeconds / scrollDuration)) / 2.0 + 0.5;
            double scrollOffset = Mth.lerp(scrollFactor, 0.0, overflowWidth);

            guiGraphics.enableScissor(startX, startY, endX, endY);
            guiGraphics.drawString(font, text, startX - (int) scrollOffset, textY, color, shadow);
            guiGraphics.disableScissor();
        } else {
            guiGraphics.drawString(font, text, startX, textY, color, shadow);
        }
    }

    public static void renderScrollingStringLeftAlign(
            GuiGraphics guiGraphics,
            Font font,
            Component text,
            int startX,
            int startY,
            int endX,
            int endY,
            int color
    ) {
        renderScrollingStringLeftAlign(guiGraphics, font, text, startX, startY, endX, endY, color, true);
    }
}
