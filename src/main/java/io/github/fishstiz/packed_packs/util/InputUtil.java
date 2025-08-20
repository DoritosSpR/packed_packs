package io.github.fishstiz.packed_packs.util;

import static com.mojang.blaze3d.platform.InputConstants.*;
import static net.minecraft.client.gui.screens.Screen.*;

public class InputUtil {
    public static final int MOUSE_BUTTON_BACK = 3;
    public static final int MOUSE_BUTTON_FORWARD = 4;
    public static final int MOD_SHIFT = 1;
    public static final int MOD_ALT = 4;

    private InputUtil() {
    }

    public static boolean isLeftClick(int button) {
        return button == MOUSE_BUTTON_LEFT;
    }

    public static boolean isRightClick(int button) {
        return button == MOUSE_BUTTON_RIGHT;
    }

    public static boolean isClickBack(int button) {
        return button == MOUSE_BUTTON_BACK;
    }

    public static boolean isClickForward(int button) {
        return button == MOUSE_BUTTON_FORWARD;
    }

    public static boolean isUndo(int keyCode, int modifiers) {
        return keyCode == KEY_Z && hasControlDown() && modifiers == MOD_CONTROL;
    }

    public static boolean isRedo(int keyCode, int modifiers) {
        if (keyCode == KEY_Z) {
            return hasControlDown() && hasShiftDown() && modifiers == MOD_CONTROL + MOD_SHIFT;
        } else if (keyCode == KEY_Y) {
            return hasControlDown() && modifiers == MOD_CONTROL;
        }

        return false;
    }

    public static boolean isTransfer(int keyCode, int modifiers) {
        return modifiers == 0 && (keyCode == KEY_SPACE || keyCode == KEY_RETURN);
    }

    public static boolean isExpandFolder(int keyCode, int modifiers) {
        return modifiers == 0 && keyCode == KEY_RETURN;
    }

    public static boolean isRangeModifierActive() {
        return hasShiftDown();
    }

    public static boolean isSelectModifierActive() {
        return hasControlDown();
    }

    public static boolean isMoveModifierActive() {
        return hasAltDown() || hasControlDown();
    }
}
