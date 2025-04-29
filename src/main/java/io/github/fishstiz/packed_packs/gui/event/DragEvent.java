package io.github.fishstiz.packed_packs.gui.event;

import io.github.fishstiz.fidgetz.gui.Background;
import io.github.fishstiz.fidgetz.gui.sprites.Sprite;
import io.github.fishstiz.packed_packs.gui.components.list.PackList;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.util.pack.PackIconCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class DragEvent extends Event implements Renderable {
    private static final Background.Color BACKGROUND = new Background.Color(Theme.GRAY_800.getARGB());
    private static final Background.Color OVERLAY = new Background.Color(Theme.BLACK.withAlpha(0.5f));
    private static final Background.Color NUM_BACKGROUND = new Background.Color(Theme.BLUE_500.getARGB());
    private static final int OFFSET = 4;
    private static final int ICON_SIZE = 48;
    private static final int NUM_SIZE = 16;
    private static final int ICON_OFFSET_X = ICON_SIZE / 2;
    private static final int ICON_OFFSET_Y = ICON_SIZE - OFFSET;
    private static final int NUM_OFFSET_X = NUM_SIZE / 2;
    private static final int NUM_OFFSET_Y = NUM_SIZE - OFFSET + (ICON_SIZE - NUM_SIZE) / 2;
    private static final double THRESHOLD = 1.0;
    private final @Unmodifiable List<Pack> dragged;
    private final Sprite sprite;

    public DragEvent(PackList target, PackIconCache iconCache) {
        super(target);

        this.dragged = target.getSelectionCopy();

        if (this.dragged.isEmpty()) {
            throw new IllegalStateException("Cannot create drag event with empty selection.");
        }

        this.sprite = Sprite.of32(iconCache.getIcon(this.dragged().getLast()));
    }

    @Override
    public boolean modifiesTarget() {
        return false;
    }

    public List<Pack> dragged() {
        return this.dragged;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        String num = String.valueOf(this.dragged().size());
        Font font = Minecraft.getInstance().font;
        int iconX = mouseX - ICON_OFFSET_X;
        int iconY = mouseY - ICON_OFFSET_Y;
        int numX = mouseX - NUM_OFFSET_X;
        int numY = mouseY - NUM_OFFSET_Y;

        BACKGROUND.render(guiGraphics, iconX, iconY, ICON_SIZE, ICON_SIZE);
        this.sprite.render(guiGraphics, iconX, iconY, ICON_SIZE, ICON_SIZE, partialTick);
        OVERLAY.render(guiGraphics, iconX, iconY, ICON_SIZE, ICON_SIZE);
        NUM_BACKGROUND.render(guiGraphics, numX, numY, NUM_SIZE, NUM_SIZE);
        guiGraphics.drawString(font, num, numX + NUM_SIZE / 2 - font.width(num) / 2, numY + NUM_SIZE / 2 - font.lineHeight / 2, Theme.WHITE.getARGB());
        guiGraphics.renderOutline(iconX, iconY, ICON_SIZE, ICON_SIZE, Theme.WHITE.getARGB());
        guiGraphics.renderOutline(numX, numY, NUM_SIZE, NUM_SIZE, Theme.WHITE.getARGB());
    }

    public static boolean exceedsThreshold(double dragX, double dragY) {
        return Math.hypot(dragX, dragY) > THRESHOLD;
    }
}
