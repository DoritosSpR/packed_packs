package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class DragEventRenderer {
    private static final int OFFSET_Y = 4;
    private static final int ICON_SIZE = 48;
    private static final int NUM_SIZE = 16;
    private static final int ICON_OFFSET_X = ICON_SIZE / 2;
    private static final int ICON_OFFSET_Y = ICON_SIZE - OFFSET_Y;
    private static final int NUM_OFFSET_Y = NUM_SIZE - OFFSET_Y + (ICON_SIZE - NUM_SIZE) / 2;
    private final ColoredRect background = new ColoredRect(Theme.GRAY_800.getARGB());
    private final ColoredRect overlay = new ColoredRect(Theme.BLACK.withAlpha(0.5f));
    private final ColoredRect numberBackground = new ColoredRect(Theme.BLUE_500.getARGB());
    private final PackAssetManager assetManager;

    public DragEventRenderer(PackAssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public void renderDragEvent(DragEvent dragEvent, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Sprite icon = this.assetManager.getIcon(dragEvent.trigger());
        String sizeString = String.valueOf(dragEvent.payload().size());
        Font font = Minecraft.getInstance().font;
        int sizeStringWidth = font.width(sizeString);
        int iconX = mouseX - ICON_OFFSET_X;
        int iconY = mouseY - ICON_OFFSET_Y;
        int numWidth = NUM_SIZE > sizeStringWidth ? NUM_SIZE : (sizeStringWidth + NUM_SIZE - font.lineHeight);
        int numX = mouseX - numWidth / 2;
        int numY = mouseY - NUM_OFFSET_Y;

        this.background.render(guiGraphics, iconX, iconY, ICON_SIZE, ICON_SIZE);
        icon.render(guiGraphics, iconX, iconY, ICON_SIZE, ICON_SIZE, partialTick);
        this.overlay.render(guiGraphics, iconX, iconY, ICON_SIZE, ICON_SIZE);
        this.numberBackground.render(guiGraphics, numX, numY, numWidth, NUM_SIZE);
        guiGraphics.drawString(font, sizeString, numX + numWidth / 2 - sizeStringWidth / 2, numY + NUM_SIZE / 2 - font.lineHeight / 2, Theme.WHITE.getARGB());
        guiGraphics.renderOutline(iconX, iconY, ICON_SIZE, ICON_SIZE, Theme.WHITE.getARGB());
        guiGraphics.renderOutline(numX, numY, numWidth, NUM_SIZE, Theme.WHITE.getARGB());
    }
}
