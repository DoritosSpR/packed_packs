package io.github.fishstiz.packed_packs.gui.components.contextmenu;

import com.google.common.util.concurrent.Runnables;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItemRenderer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.RenderableMenuItem;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.repository.Pack;

public record PackMenuHeader(Pack pack, Font font, Sprite sprite) implements MenuItemRenderer {
    public PackMenuHeader(Pack pack, Sprite sprite) {
        this(pack, Minecraft.getInstance().font, sprite);
    }

    public static RenderableMenuItem withItem(Pack pack, Sprite sprite) {
        return new RenderableMenuItem(new PackMenuHeader(pack, sprite), false, false, Runnables.doNothing());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, int spacing, boolean hovered, double mouseX, double mouseY, float partialTick) {
        guiGraphics.fill(x, y, x + width, y + height, Theme.GRAY_500.getARGB());

        int size = this.font.lineHeight;

        int innerX = x + spacing;
        int innerY = y + spacing;
        int innerWidth = width - 2 * spacing;
        int innerHeight = height - 2 * spacing;

        int iconY = innerY + (innerHeight - size) / 2;
        this.sprite.render(guiGraphics, innerX, iconY, size, size, partialTick);

        int textX = innerX + size + spacing;
        DrawUtil.renderScrollingStringLeftAlign(
                guiGraphics,
                this.font,
                pack.getTitle(),
                textX, y + 1,
                innerX + innerWidth, y + height,
                Theme.WHITE.getARGB()
        );
    }
}
