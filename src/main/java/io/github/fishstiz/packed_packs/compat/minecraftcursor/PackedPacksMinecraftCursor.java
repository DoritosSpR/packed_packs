package io.github.fishstiz.packed_packs.compat.minecraftcursor;

import io.github.fishstiz.minecraftcursor.api.*;
import io.github.fishstiz.packed_packs.gui.components.events.DragEvent;
import io.github.fishstiz.packed_packs.gui.components.pack.AvailablePackList;
import io.github.fishstiz.packed_packs.gui.components.pack.CurrentPackList;
import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public class PackedPacksMinecraftCursor implements MinecraftCursorInitializer {
    @Override
    public void init(CursorTypeRegistrar cursorRegistrar, ElementRegistrar elementRegistrar) {
        elementRegistrar.register(AvailablePackList.Entry.class, this::getCursorType);
        elementRegistrar.register(CurrentPackList.Entry.class, this::getCursorType);

        ScreenEvents.AFTER_INIT.register(((minecraft, screen, width, height) -> {
            if (screen instanceof PackedPacksScreen) {
                ScreenEvents.afterRender(screen).register((packScreen, guiGraphics, mouseX, mouseY, partialTick) ->
                        this.updateDraggingCursor((PackedPacksScreen) packScreen, mouseX, mouseY)
                );
            }
        }));
    }

    private CursorType getCursorType(AvailablePackList.Entry entry, double mouseX, double mouseY) {
        return entry.isMouseOverSelect(mouseX, mouseY) ? CursorType.POINTER : CursorType.DEFAULT;
    }

    private CursorType getCursorType(CurrentPackList.Entry entry, double mouseX, double mouseY) {
        if (entry.isMouseOverRemove(mouseX, mouseY) || entry.isMouseOverUp(mouseX, mouseY) || entry.isMouseOverDown(mouseX, mouseY)) {
            return CursorType.POINTER;
        }
        return CursorType.DEFAULT;
    }

    private void updateDraggingCursor(PackedPacksScreen screen, double mouseX, double mouseY) {
        DragEvent dragEvent = screen.getDragged();
        if (dragEvent != null) {
            PackListBase source = dragEvent.target();
            boolean validDrop = false;

            for (PackListBase destination : screen.getPackLists()) {
                if (destination.isMouseOver(mouseX, mouseY)) {
                    validDrop = source == destination ||
                                destination instanceof CurrentPackList scrollable && scrollable.isScrolling() ||
                                destination.canDrop(dragEvent.target(), dragEvent.payload(), dragEvent.trigger(), mouseX, mouseY);
                    break;
                }
            }
            CursorController.getInstance().setSingleCycleCursor(validDrop ? CursorType.GRABBING : CursorType.NOT_ALLOWED);
        }
    }
}
