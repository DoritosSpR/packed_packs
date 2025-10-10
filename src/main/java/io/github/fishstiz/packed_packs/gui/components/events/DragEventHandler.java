package io.github.fishstiz.packed_packs.gui.components.events;

import io.github.fishstiz.packed_packs.gui.components.pack.PackListBase;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
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
        if (child.isPresent() && child.get() instanceof PackListBase packList && !packList.isLocked()) {
            packList.drop(event.target(), event.payload(), event.trigger(), mouseX, mouseY);
        }
    }

    @Override
    default boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        if (this.isDraggingSelection()) {
            return true;
        }

        return ContainerEventHandler.super.mouseDragged(mouseButtonEvent, dragX, dragY);
    }

    @Override
    default boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        DragEvent event = this.getDragged();

        if (isLeftClick(mouseButtonEvent) && event != null) {
            this.onRelease(event, mouseButtonEvent.x(), mouseButtonEvent.y());
            this.setDragged(null);
            return true;
        }

        return ContainerEventHandler.super.mouseReleased(mouseButtonEvent);
    }
}
