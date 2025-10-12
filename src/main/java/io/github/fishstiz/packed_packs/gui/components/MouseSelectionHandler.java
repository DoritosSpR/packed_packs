package io.github.fishstiz.packed_packs.gui.components;

import net.minecraft.Util;
import net.minecraft.client.gui.components.events.GuiEventListener;

import static io.github.fishstiz.packed_packs.util.InputUtil.*;

public class MouseSelectionHandler<T> {
    private static final double DRAG_THRESHOLD = 1.0;
    private final GuiEventListener inputListener;
    private final SelectionContext<T> context;
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

    public MouseSelectionHandler(GuiEventListener inputListener, SelectionContext<T> context) {
        this.inputListener = inputListener;
        this.context = context;
    }

    private static boolean exceedsDragThreshold(double dragX, double dragY) {
        return Math.hypot(dragX, dragY) > DRAG_THRESHOLD;
    }

    private boolean updateDoubleClick() {
        long currentTime = Util.getMillis();
        boolean doubleClicked = (currentTime - this.lastClickTime) < DOUBLE_CLICK_THRESHOLD_MS;
        this.lastClickTime = currentTime;
        return doubleClicked;
    }

    public Action mouseClicked(double mouseX, double mouseY, int button) {
        if (!isLeftClick(button) || !this.inputListener.isMouseOver(mouseX, mouseY)) {
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
        if (!this.context.isSelected()) {
            this.mouseSelectionState = MouseSelectionState.SELECTING_ONE;
            return Action.SELECT_EXCLUSIVE;
        }
        if (!this.context.isSelectedLast()) {
            this.mouseSelectionState = MouseSelectionState.SELECTING_ONE;
            return Action.SELECT;
        }

        this.mouseSelectionState = MouseSelectionState.SELECTING_ONE;
        return Action.FOCUS;
    }

    public Action mouseReleased(double mouseX, double mouseY, int button) {
        if (this.inputListener.isMouseOver(mouseX, mouseY)
            && this.mouseSelectionState == MouseSelectionState.SELECTING_ONE
            && this.context.isSelectedLast()
            && this.context.selection().size() > 1) {
            this.mouseSelectionState = MouseSelectionState.INACTIVE;
            return Action.SELECT_EXCLUSIVE;
        }
        this.mouseSelectionState = MouseSelectionState.INACTIVE;
        return Action.NONE;
    }

    public Action mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.inputListener.isMouseOver(mouseX, mouseY)) {
            this.mouseSelectionState = MouseSelectionState.INACTIVE;
            return Action.NONE;
        }

        if (exceedsDragThreshold(dragX, dragY) &&
            this.context.isSelected() &&
            this.mouseSelectionState == MouseSelectionState.SELECTING_ONE) {
            return Action.DRAG;
        }

        return Action.NONE;
    }
}
