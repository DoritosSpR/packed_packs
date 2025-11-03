package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static io.github.fishstiz.packed_packs.util.InputUtil.isLeftClick;

public interface DragEventHandler extends ContainerEventHandler {
    @Nullable DragEvent getDragged();

    void setDragged(DragEvent dragged);

    default boolean isDraggingSelection() {
        return this.getDragged() != null;
    }

    default void handleDragEvent(DragEvent event) {
        if (!this.isDraggingSelection()) {
            this.setDragged(event);
        }
    }

    default void onRelease(@NotNull DragEvent event, double mouseX, double mouseY) {
        Optional<GuiEventListener> child = this.getChildAt(mouseX, mouseY);
        if (child.isPresent() && child.get() instanceof PackList packList) {
            packList.drop(event, mouseX, mouseY);
        }
    }

    @Override
    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDraggingSelection()) {
            return true;
        }

        return ContainerEventHandler.super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        DragEvent event = this.getDragged();

        if (isLeftClick(button) && event != null) {
            this.onRelease(event, mouseX, mouseY);
            this.setDragged(null);
            return true;
        }

        return ContainerEventHandler.super.mouseReleased(mouseX, mouseY, button);
    }

    @Deprecated(since = "mc1.21.6")
    default float getDroppableZ() {
        return 1f;
    }
}
