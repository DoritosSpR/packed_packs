package io.github.fishstiz.packed_packs.util.constants;

import io.github.fishstiz.fidgetz.util.ARGBColor;

public enum Theme implements ARGBColor {
    RED_700(0xFFB00000),
    RED_900(0xFF770000),
    GREEN_500(0xFF22C55E),
    BLUE_500(0xFF3B82F6),
    PURPLE_500(0xFF8B5CF6),
    GRAY_500(0xFF808080),
    GRAY_800(0xFF3F3F3F),
    BLACK(0xFF000000),
    WHITE(0xFFFFFFFF);

    private final int argb;

    Theme(int argb) {
        this.argb = argb;
    }

    @Override
    public int getARGB() {
        return this.argb;
    }
}
