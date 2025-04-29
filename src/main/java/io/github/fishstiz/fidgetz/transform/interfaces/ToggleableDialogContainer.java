package io.github.fishstiz.fidgetz.transform.interfaces;

import io.github.fishstiz.fidgetz.gui.components.ToggleableDialog;
import net.minecraft.client.gui.layouts.LayoutElement;

import java.util.List;

public interface ToggleableDialogContainer {
    void fidgetz$trackDialogVisibility(ToggleableDialog<? extends LayoutElement, ?> dialog);

    List<ToggleableDialog<? extends LayoutElement, ?>> fidgetz$getVisibleDialogs();
}
