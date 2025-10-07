package io.github.fishstiz.packed_packs.util.constants;

import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.packed_packs.util.ResourceUtil;

public class GuiConstants {
    public static final int SPACING = 8;
    public static final ColoredRect WHITE_OVERLAY = new ColoredRect(Theme.WHITE.withAlpha(0.25f));
    public static final Sprite CROSS_SPRITE = Sprite.of16(ResourceUtil.getIcon("cross"));
    public static final Sprite HAMBURGER_SPRITE = Sprite.of16(ResourceUtil.getIcon("hamburger"));
    public static final Sprite LOCK_SPRITE = new Sprite(ResourceUtil.getVanillaSprite("widget/locked_button_disabled"), Size.of16());
    public static final ColoredRect DEVELOPER_MODE_ITEM_BACKGROUND = new ColoredRect(Theme.BLACK.withAlpha(0.25f));

    private GuiConstants() {
    }
}
