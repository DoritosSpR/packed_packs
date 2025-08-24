package io.github.fishstiz.fidgetz.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
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
                if (button == InputConstants.MOUSE_BUTTON_LEFT) this.setDragging(true);
                return true;
            }
            return false;
        }).orElse(false);
    }
}
