package io.github.fishstiz.fidgetz.gui.components;

import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public interface ToggleableDialogContainer {
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
            if (!isEnclosed && dialog.children().contains(child)) {
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
            if (!isIntersected && dialog.children().contains(child)) {
                isDialogChild = true;
                break;
            }
            if (!isIntersected && dialog != child && dialog.isMouseOver(px, py) && dialog.intersects(child)) {
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
}
