package io.github.fishstiz.fidgetz.gui.components;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;

public interface ContainerEventHandlerPatch extends ContainerEventHandler {
    /**
     * In newer versions of minecraft, {@link ContainerEventHandler#mouseClicked(double, double, int)}
     * always returns {@code true} if {@link ContainerEventHandler#getChildAt(double, double)} is present.
     * <p>
     * This code is copied from the 1.21.1 version of {@link ContainerEventHandler#mouseClicked(double, double, int)}
     */
    @Override
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(guieventlistener);
                if (button == 0) {
                    this.setDragging(true);
                }

                return true;
            }
        }
        return false;
    }
}
