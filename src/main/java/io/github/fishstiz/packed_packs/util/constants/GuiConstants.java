package io.github.fishstiz.packed_packs.util.constants;

import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.util.ResourceUtil;

public class GuiConstants {
    public static final int SPACING = 8;
    public static final ColoredRect WHITE_OVERLAY = new ColoredRect(Theme.WHITE.withAlpha(0.25f));
    public static final Sprite CROSS_SPRITE = Sprite.of16(ResourceUtil.getIcon("cross"));
    public static final Sprite HAMBURGER_SPRITE = Sprite.of16(ResourceUtil.getIcon("hamburger"));

    private GuiConstants() {
    }
}
