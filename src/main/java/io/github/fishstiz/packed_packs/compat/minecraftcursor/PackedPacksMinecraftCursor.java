package io.github.fishstiz.packed_packs.compat.minecraftcursor;

import io.github.fishstiz.minecraftcursor.api.*;
import io.github.fishstiz.packed_packs.gui.components.AvailablePackList;
import io.github.fishstiz.packed_packs.gui.components.CurrentPackList;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;

public class PackedPacksMinecraftCursor implements MinecraftCursorInitializer {
    @Override
    public void init(CursorTypeRegistrar cursorRegistrar, ElementRegistrar elementRegistrar) {
        elementRegistrar.register(AvailablePackList.Entry.class, this::getCursorType);
        elementRegistrar.register(CurrentPackList.Entry.class, this::getCursorType);
        elementRegistrar.register(PackedPacksScreen.class, this::getCursorType);
    }

    private CursorType getCursorType(AvailablePackList.Entry entry, double mouseX, double mouseY) {
        return entry.isMouseOverSelect(mouseX, mouseY) ? CursorType.POINTER : CursorType.DEFAULT;
    }

    private CursorType getCursorType(CurrentPackList.Entry entry, double mouseX, double mouseY) {
        if (entry.isMouseOverRemove(mouseX, mouseY)
            || entry.isMouseOverUp(mouseX, mouseY)
            || entry.isMouseOverDown(mouseX, mouseY)) {
            return CursorType.POINTER;
        }
        return CursorType.DEFAULT;
    }

    private CursorType getCursorType(PackedPacksScreen screen, double mouseX, double mouseY) {
        return screen.isDraggingSelection() ? CursorType.GRABBING : CursorType.DEFAULT;
    }
}
