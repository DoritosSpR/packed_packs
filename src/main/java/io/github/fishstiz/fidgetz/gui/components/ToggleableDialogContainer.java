package io.github.fishstiz.fidgetz.gui.components;

import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.ArrayList;
import java.util.List;

import static io.github.fishstiz.fidgetz.util.GuiUtil.isDescendant;

public interface ToggleableDialogContainer extends ContainerEventHandlerPatch {
    List<ToggleableDialog<?>> getDialogs();

    default ArrayList<ToggleableDialog<?>> getOpenDialogs() {
        List<ToggleableDialog<?>> dialogs = this.getDialogs();
        ArrayList<ToggleableDialog<?>> openDialogs = new ArrayList<>(dialogs.size());

        for (ToggleableDialog<?> dialog : dialogs) {
            if (dialog.isOpen()) {
                openDialogs.add(dialog);
            }
        }

        return openDialogs;
    }

    default boolean isChildCovered(GuiEventListener child) {
        boolean isDialogChild = false;
        boolean isEnclosed = false;

        for (ToggleableDialog<?> dialog : this.getOpenDialogs()) {
            if (dialog != child && (dialog.isCaptureClick() || dialog.isCaptureFocus()) && !isDescendant(dialog, child)) {
                return true;
            }
            if (!isEnclosed && isDescendant(dialog, child)) {
                isDialogChild = true;
                break;
            }
            if (!isEnclosed && dialog != child && dialog.encloses(child)) {
                isEnclosed = true;
            }
        }

        return !isDialogChild && isEnclosed;
    }

    default boolean isChildCoveredAtPoint(GuiEventListener child, double px, double py) {
        boolean isDialogChild = false;
        boolean isIntersected = false;

        for (ToggleableDialog<?> dialog : this.getOpenDialogs()) {
            if (dialog != child && (dialog.isCaptureClick() || dialog.isCaptureFocus()) && !isDescendant(dialog, child)) {
                return true;
            }
            if (!isIntersected && isDescendant(dialog, child)) {
                isDialogChild = true;
                break;
            }
            if (!isIntersected && dialog != child && dialog.isMouseOverBounds(px, py) && dialog.intersects(child)) {
                isIntersected = true;
            }
        }

        return !isDialogChild && isIntersected;
    }
}
