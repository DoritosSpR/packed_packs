package io.github.fishstiz.fidgetz.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;

public interface ContainerEventHandlerPatch extends ContainerEventHandler {
    /**
     * This code is copied from the 1.21.1 version of {@link ContainerEventHandler#mouseClicked}
     */
    @Override
    default boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        for (GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener.mouseClicked(mouseButtonEvent, doubleClicked)) {
                if (guieventlistener.shouldTakeFocusAfterInteraction()) {
                    this.setFocused(guieventlistener);
                }
                if (mouseButtonEvent.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                    this.setDragging(true);
                }

                return true;
            }
        }
        return false;
    }

    /**
     * {@link ContainerEventHandler#mouseClicked}, except it only
     * returns {@code true} if mouse click is actually handled instead of when child is present.
     */
    default boolean mouseClickedAt(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        return this.getChildAt(mouseButtonEvent.x(), mouseButtonEvent.y()).map(child -> {
            if (child.mouseClicked(mouseButtonEvent, doubleClicked)) {
                if (child.shouldTakeFocusAfterInteraction()) {
                    this.setFocused(child);
                }
                if (mouseButtonEvent.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                    this.setDragging(true);
                }
                return true;
            }
            return false;
        }).orElse(false);
    }
}
