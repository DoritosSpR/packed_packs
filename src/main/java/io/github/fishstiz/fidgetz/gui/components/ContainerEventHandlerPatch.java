package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.packed_packs.util.InputUtil;
import net.minecraft.client.gui.components.events.ContainerEventHandler;

public interface ContainerEventHandlerPatch extends ContainerEventHandler {
    /**
     * {@link ContainerEventHandler#mouseClicked(double, double, int)}, except it only
     * propagates to the hovered child
     */
    default boolean mouseClickedAt(double mouseX, double mouseY, int button) {
        return this.getChildAt(mouseX, mouseY).map(child -> {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(child);
                if (InputUtil.isLeftClick(button)) this.setDragging(true);
                return true;
            }
            return false;
        }).orElse(false);
    }
}
