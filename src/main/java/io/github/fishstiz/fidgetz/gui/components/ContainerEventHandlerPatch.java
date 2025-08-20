package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.packed_packs.util.InputUtil;
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

    /**
     * {@link ContainerEventHandler#mouseClicked(double, double, int)}, except it only
     * returns true if mouse click is actually handled instead of when child is present.
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
