package io.github.fishstiz.packed_packs.gui.components.events;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.server.packs.repository.Pack;

import java.util.List;

public final class DragEvent extends PackListEvent implements Renderable {
    private static final ColoredRect BACKGROUND = new ColoredRect(Theme.GRAY_800.getARGB());
    private static final ColoredRect OVERLAY = new ColoredRect(Theme.BLACK.withAlpha(0.5f));
    private static final ColoredRect NUM_BACKGROUND = new ColoredRect(Theme.BLUE_500.getARGB());
    private static final int OFFSET_Y = 4;
    private static final int ICON_SIZE = 48;
    private static final int NUM_SIZE = 16;
    private static final int ICON_OFFSET_X = ICON_SIZE / 2;
    private static final int ICON_OFFSET_Y = ICON_SIZE - OFFSET_Y;
    private static final int NUM_OFFSET_Y = NUM_SIZE - OFFSET_Y + (ICON_SIZE - NUM_SIZE) / 2;
    private final ImmutableList<Pack> payload;
    private final Pack trigger;
    private final Sprite sprite;

    public DragEvent(PackList target, List<Pack> selection, Pack trigger, Sprite sprite) {
        super(target);

        if (selection.isEmpty()) {
            throw new IllegalStateException("Cannot create drag event with empty selection.");
        }

        this.payload = ImmutableList.copyOf(selection);
        this.trigger = trigger;
        this.sprite = sprite;
    }

    @Override
    public boolean pushToHistory() {
        return false;
    }

    public ImmutableList<Pack> payload() {
        return this.payload;
    }

    public Pack trigger() {
        return this.trigger;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        String sizeString = String.valueOf(this.payload().size());
        Font font = Minecraft.getInstance().font;
        int sizeStringWidth = font.width(sizeString);
        int iconX = mouseX - ICON_OFFSET_X;
        int iconY = mouseY - ICON_OFFSET_Y;
        int numWidth = NUM_SIZE > sizeStringWidth ? NUM_SIZE : (sizeStringWidth + NUM_SIZE - font.lineHeight);
        int numX = mouseX - numWidth / 2;
        int numY = mouseY - NUM_OFFSET_Y;

        BACKGROUND.render(guiGraphics, iconX, iconY, ICON_SIZE, ICON_SIZE);
        this.sprite.render(guiGraphics, iconX, iconY, ICON_SIZE, ICON_SIZE, partialTick);
        OVERLAY.render(guiGraphics, iconX, iconY, ICON_SIZE, ICON_SIZE);
        NUM_BACKGROUND.render(guiGraphics, numX, numY, numWidth, NUM_SIZE);
        guiGraphics.drawString(font, sizeString, numX + numWidth / 2 - sizeStringWidth / 2, numY + NUM_SIZE / 2 - font.lineHeight / 2, Theme.WHITE.getARGB());
        guiGraphics.renderOutline(iconX, iconY, ICON_SIZE, ICON_SIZE, Theme.WHITE.getARGB());
        guiGraphics.renderOutline(numX, numY, numWidth, NUM_SIZE, Theme.WHITE.getARGB());
    }
}
