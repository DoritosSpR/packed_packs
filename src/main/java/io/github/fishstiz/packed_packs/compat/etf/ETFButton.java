package io.github.fishstiz.packed_packs.compat.etf;

import io.github.fishstiz.fidgetz.gui.components.SpriteButton;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.packed_packs.compat.ModAdditions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import traben.entity_texture_features.config.screens.ETFConfigScreenMain;
import traben.entity_texture_features.utils.ETFUtils2;

public class ETFButton {
    private static final Size SIZE = new Size(24, 20);
    private static final Sprite UNFOCUSED = new Sprite(ETFUtils2.res("entity_features", "textures/gui/settings_unfocused.png"), SIZE);
    private static final Sprite FOCUSED = new Sprite(ETFUtils2.res("entity_features", "textures/gui/settings_focused.png"), SIZE);

    private ETFButton() {
    }

    public static SpriteButton<Void> getButton(Screen previous) {
        return SpriteButton.<Void>builder(SpriteButton.Sprites.of(new ButtonSprites(FOCUSED, UNFOCUSED)))
                .setMessage(ModAdditions.Mod.ETF.getId())
                .setDimensions(SIZE.width(), SIZE.height())
                .setOnPress(() -> Minecraft.getInstance().setScreen(new ETFConfigScreenMain(previous)))
                .build();
    }
}
