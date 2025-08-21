package io.github.fishstiz.fidgetz.gui.renderables;

import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.util.lang.ObjectsUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

public record CenteredTextRect(Component text, Font font, int color, boolean shadow) implements RenderableRect {
    public CenteredTextRect(Component text, boolean shadow) {
        this(text, Minecraft.getInstance().font, ObjectsUtil.mapOrDefault(text.getStyle().getColor(), Theme.WHITE.getARGB(), TextColor::getValue), shadow);
    }

    public CenteredTextRect(String text, boolean shadow) {
        this(Component.literal(text), shadow);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        int textWidth = this.font.width(this.text);
        int textHeight = this.font.lineHeight;

        int drawX = x + (width - textWidth) / 2;
        int drawY = y + (height - textHeight) / 2;

        guiGraphics.drawString(this.font, this.text, drawX, drawY, this.color, this.shadow);
    }
}
