package io.github.fishstiz.fidgetz.gui.components;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static io.github.fishstiz.fidgetz.util.GuiUtil.isDescendant;

public interface ToggleableDialogContainer extends ContainerEventHandler {
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

        for (ToggleableDialog<?> dialog : this.getOpenDialogsFromTop()) {
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

        for (ToggleableDialog<?> dialog : this.getOpenDialogsFromTop()) {
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

    private List<ToggleableDialog<?>> getOpenDialogsFromTop() {
        ArrayList<ToggleableDialog<?>> dialogs = this.getOpenDialogs();
        dialogs.sort(Comparator.<ToggleableDialog<?>, Float>comparing(ToggleableDialog::getZ).reversed());
        return dialogs;
    }

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
