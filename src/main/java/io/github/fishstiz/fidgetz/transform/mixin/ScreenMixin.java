package io.github.fishstiz.fidgetz.transform.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.gui.components.ToggleableDialog;
import io.github.fishstiz.fidgetz.transform.interfaces.ToggleableDialogContainer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin implements ToggleableDialogContainer {
    @Unique
    private final List<ToggleableDialog<? extends LayoutElement, ?>> fidgetz$visibleDialogs = new ArrayList<>();

    @Override
    public List<ToggleableDialog<? extends LayoutElement, ?>> fidgetz$getVisibleDialogs() {
        this.fidgetz$visibleDialogs.removeIf(dialog -> dialog == null || !dialog.isOpen());
        return this.fidgetz$visibleDialogs;
    }

    @Override
    public void fidgetz$trackDialogVisibility(ToggleableDialog<? extends LayoutElement, ?> dialog) {
        if (dialog.isOpen()) {
            this.fidgetz$visibleDialogs.add(dialog);
        } else {
            this.fidgetz$visibleDialogs.remove(dialog);
        }
    }

    @WrapMethod(method = "children")
    public List<? extends GuiEventListener> getUncoveredChildren(Operation<List<? extends GuiEventListener>> original) {
        var children = original.call();

        if (!this.fidgetz$getVisibleDialogs().isEmpty()) {
            List<GuiEventListener> childrenCopy = new ArrayList<>(children);
            List<GuiEventListener> dialogChildren = new ArrayList<>();

            for (var dialog : this.fidgetz$getVisibleDialogs()) {
                // noinspection Java8CollectionRemoveIf
                for (Iterator<GuiEventListener> it = childrenCopy.iterator(); it.hasNext(); ) {
                    var child = it.next();
                    if (child instanceof LayoutElement widget && dialog.isCovering(widget)) {
                        it.remove();
                    }
                }
                dialogChildren.add(dialog);
                dialogChildren.addAll(dialog.children());
            }

            dialogChildren.addAll(childrenCopy);
            return dialogChildren;
        }
        return children;
    }

    @WrapMethod(method = "keyPressed")
    public boolean closeDialog(int keyCode, int scanCode, int modifiers, Operation<Boolean> original) {
        if (!this.fidgetz$getVisibleDialogs().isEmpty() && keyCode == InputConstants.KEY_ESCAPE) {
            for (var dialog : this.fidgetz$getVisibleDialogs()) {
                if (dialog.shouldCloseOnEscape()) {
                    dialog.setOpen(false);
                    return true;
                }
            }
        }
        return original.call(keyCode, scanCode, modifiers);
    }
}
