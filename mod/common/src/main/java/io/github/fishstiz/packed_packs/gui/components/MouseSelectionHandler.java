package io.github.fishstiz.packed_packs.gui.components;

import net.minecraft.util.Util;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;
import java.util.Objects;

import static io.github.fishstiz.packed_packs.util.InputUtil.*;

public class MouseSelectionHandler<T> {
    private static final double DRAG_THRESHOLD = 1.0;
    private final GuiEventListener inputListener;
    private final List<T> selection;
    private final T item;
    private MouseSelectionState mouseSelectionState = MouseSelectionState.INACTIVE;
    private long lastClickTime = 0;

    public enum Action {
        NONE(false),
        FOCUS(false),
        SELECT(true),
        SELECT_TOGGLE(true),
        SELECT_EXCLUSIVE(true),
        SELECT_RANGE(true),
        TRANSFER(false),
        DRAG(false);

        private final boolean select;

        Action(boolean select) {
            this.select = select;
        }

        public boolean shouldDispatch() {
            return this != NONE;
        }

        public boolean shouldSelect() {
            return this.select;
        }
    }

    private enum MouseSelectionState {
        INACTIVE,
        SELECTING_ONE,
        SELECTING_MANY
    }

    public MouseSelectionHandler(GuiEventListener inputListener, List<T> selection, T item) {
        this.inputListener = inputListener;
        this.selection = selection;
        this.item = item;
    }

    private static boolean exceedsDragThreshold(double dragX, double dragY) {
        return Math.hypot(dragX, dragY) > DRAG_THRESHOLD;
    }

    private boolean isSelected() {
        return this.selection.contains(this.item);
    }

    private boolean isSelectedLast() {
        return !this.selection.isEmpty() && Objects.equals(this.selection.getLast(), this.item) ;
    }

    private boolean updateDoubleClick() {
        long currentTime = Util.getMillis();
        boolean doubleClicked = (currentTime - this.lastClickTime) < DOUBLE_CLICK_THRESHOLD_MS;
        this.lastClickTime = currentTime;
        return doubleClicked;
    }

    public Action mouseClicked(MouseButtonEvent mouseButtonEvent) {
        if (!isLeftClick(mouseButtonEvent) || !this.inputListener.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            this.mouseSelectionState = MouseSelectionState.INACTIVE;
            return Action.NONE;
        }
        if (!isRangeModifierActive() && !isSelectModifierActive() && this.updateDoubleClick()) {
            return Action.TRANSFER;
        }
        if (isRangeModifierActive()) {
            this.mouseSelectionState = MouseSelectionState.SELECTING_MANY;
            return Action.SELECT_RANGE;
        }
        if (isSelectModifierActive()) {
            this.mouseSelectionState = MouseSelectionState.SELECTING_MANY;
            return Action.SELECT_TOGGLE;
        }
        if (!this.isSelected()) {
            this.mouseSelectionState = MouseSelectionState.SELECTING_ONE;
            return Action.SELECT_EXCLUSIVE;
        }
        if (!this.isSelectedLast()) {
            this.mouseSelectionState = MouseSelectionState.SELECTING_ONE;
            return Action.SELECT;
        }

        this.mouseSelectionState = MouseSelectionState.SELECTING_ONE;
        return Action.FOCUS;
    }

    public Action mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (this.inputListener.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())
            && this.mouseSelectionState == MouseSelectionState.SELECTING_ONE
            && this.isSelectedLast()
            && this.selection.size() > 1) {
            this.mouseSelectionState = MouseSelectionState.INACTIVE;
            return Action.SELECT_EXCLUSIVE;
        }
        this.mouseSelectionState = MouseSelectionState.INACTIVE;
        return Action.NONE;
    }

    public Action mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        if (!this.inputListener.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            this.mouseSelectionState = MouseSelectionState.INACTIVE;
            return Action.NONE;
        }

        if (exceedsDragThreshold(dragX, dragY) &&
            this.isSelected() &&
            this.mouseSelectionState == MouseSelectionState.SELECTING_ONE) {
            return Action.DRAG;
        }

        return Action.NONE;
    }
}
