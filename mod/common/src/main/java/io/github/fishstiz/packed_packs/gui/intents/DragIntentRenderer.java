package io.github.fishstiz.packed_packs.gui.intents;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.packed_packs.gui.components.pack.FolderDialog;
import io.github.fishstiz.packed_packs.gui.components.pack.PackListContainer;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jspecify.annotations.Nullable;

public final class DragIntentRenderer {
    private static final int OFFSET_Y = 4;
    private static final int ICON_SIZE = 48;
    private static final int NUM_SIZE = 16;
    private static final int ICON_OFFSET_X = ICON_SIZE / 2;
    private static final int ICON_OFFSET_Y = ICON_SIZE - OFFSET_Y;
    private static final int NUM_OFFSET_Y = NUM_SIZE - OFFSET_Y + (ICON_SIZE - NUM_SIZE) / 2;
    private final ColoredRect background = new ColoredRect(Theme.GRAY_800.getARGB());
    private final ColoredRect overlay = new ColoredRect(Theme.BLACK.withAlpha(0.5f));
    private final ColoredRect numberBackground = new ColoredRect(Theme.BLUE_500.getARGB());

    public void render(
            PackListContainer available,
            PackListContainer enabled,
            PackListIntent.@Nullable Drag dragged,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (dragged == null) return;

        boolean validDrop = false;

        PackListViewModel source = dragged.source();
        validDrop |= this.renderDroppableZone(source, available, dragged, guiGraphics, mouseX, mouseY, partialTick);
        validDrop |= this.renderDroppableZone(source, enabled, dragged, guiGraphics, mouseX, mouseY, partialTick);

        this.renderDragIntent(dragged, guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.requestCursor(validDrop ? CursorTypes.RESIZE_ALL : CursorTypes.NOT_ALLOWED);
    }

    private boolean renderDroppableZone(
            PackListViewModel source,
            PackListContainer listContainer,
            PackListIntent.Drag dragged,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        FolderDialog folderDialog = listContainer.folder();
        if (folderDialog != null) {
            return this.renderDroppableZone(source, folderDialog.root(), dragged, guiGraphics, mouseX, mouseY, partialTick);
        }

        listContainer.list().renderDroppableZone(guiGraphics, source, dragged.pack(), dragged.payload(), mouseX, mouseY, partialTick);
        if (listContainer.isMouseOver(mouseX, mouseY)) {
            return source == listContainer.getViewModel() || source.canInteract(listContainer.getViewModel());
        }
        return false;
    }

    private void renderDragIntent(PackListIntent.@Nullable Drag dragged, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Sprite icon = dragged.sourceEntry().sprite();
        String sizeString = dragged.size();
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
        DrawUtil.renderOutline(guiGraphics, iconX, iconY, ICON_SIZE, ICON_SIZE, Theme.WHITE.getARGB());
        DrawUtil.renderOutline(guiGraphics, numX, numY, numWidth, NUM_SIZE, Theme.WHITE.getARGB());
    }
}
