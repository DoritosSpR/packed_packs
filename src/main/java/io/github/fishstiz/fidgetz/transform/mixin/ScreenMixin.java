package io.github.fishstiz.fidgetz.transform.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @WrapMethod(method = "children")
    public List<? extends GuiEventListener> getUncoveredChildren(Operation<List<? extends GuiEventListener>> original) {
        return original.call();
//        var children = original.call();
//
//        if (!(this instanceof ToggleableDialogScreen container)) return children;
//
//        List<ToggleableDialog<?, ?>> visibleDialogs = container.getVisibleDialogs();
//        if (visibleDialogs.isEmpty()) return children;
//
//        List<GuiEventListener> uncoveredChildren = new ArrayList<>(children);
//        List<GuiEventListener> sortedChildren = new ArrayList<>();
//
//        for (var dialog : visibleDialogs) {
//            // noinspection Java8CollectionRemoveIf
//            for (Iterator<GuiEventListener> it = uncoveredChildren.iterator(); it.hasNext(); ) {
//                GuiEventListener child = it.next();
//                if (child instanceof LayoutElement widget && dialog.isCovering(widget)) {
//                    it.remove();
//                }
//            }
//            sortedChildren.add(dialog);
//            sortedChildren.addAll(dialog.children());
//        }
//        sortedChildren.addAll(uncoveredChildren);
//        return sortedChildren;
    }
//
//    @WrapMethod(method = "mouse")
//
//    @WrapMethod(method = "keyPressed")
//    public boolean closeDialog(int keyCode, int scanCode, int modifiers, Operation<Boolean> original) {
//        if (this instanceof ToggleableDialogScreen container) {
//            List<ToggleableDialog<?, ?>> visibleDialogs = container.getVisibleDialogs();
//            if (keyCode == InputConstants.KEY_ESCAPE) {
//                for (var dialog : visibleDialogs) {
//                    if (dialog.shouldCloseOnEscape()) {
//                        dialog.setOpen(false);
//                        return true;
//                    }
//                }
//            }
//        }
//
//        return original.call(keyCode, scanCode, modifiers);
//    }
}
