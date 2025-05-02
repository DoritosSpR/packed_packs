package io.github.fishstiz.packed_packs.compat.minecraftcursor;

import io.github.fishstiz.minecraftcursor.api.CursorHandler;
import io.github.fishstiz.minecraftcursor.api.CursorType;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.gui.components.Sidebar;
import net.minecraft.client.gui.components.events.GuiEventListener;

public class PackedPacksScreenHandler implements CursorHandler<PackedPacksScreen> {
    @Override
    public CursorType getCursorType(PackedPacksScreen packsScreen, double mouseX, double mouseY) {
        GuiEventListener child = packsScreen.getChildAt(mouseX, mouseY).orElse(null);

        if (packsScreen.isDraggingSelection()) {
            return CursorType.GRABBING;
        }
        if (child instanceof Sidebar<?, ?> sidebar
            && sidebar.isMouseOver(mouseX, mouseY)
            && sidebar.getChildAt(mouseX, mouseY).isEmpty()) {
            return CursorType.DEFAULT_FORCE;
        }

        return null;
    }
}
