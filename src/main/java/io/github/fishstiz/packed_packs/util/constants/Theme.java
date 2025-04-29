package io.github.fishstiz.packed_packs.util.constants;

import io.github.fishstiz.fidgetz.util.ITheme;

public enum Theme implements ITheme {
    RED_700(0xFFB00000), // Turkey Red
    RED_900(0xFF770000), // Barn Red
    GREEN_500(0xFF22C55E), // Dark Pastel Green
    BLUE_500(0xFF3B82F6), // Azure
    GRAY_500(0xFF808080), // Gray
    GRAY_800(0xFF3F3F3F), // Onyx
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
