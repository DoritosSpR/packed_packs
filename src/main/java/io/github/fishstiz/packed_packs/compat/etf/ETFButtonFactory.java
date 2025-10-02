package io.github.fishstiz.packed_packs.compat.etf;

import io.github.fishstiz.fidgetz.gui.components.SpriteButton;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModScreenFactory;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.gui.metadata.Toggleable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

public class ETFButtonFactory {
    private static final String NAMESPACE = "entity_features";
    private static final String ETF_SCREEN_NAME = "traben.entity_texture_features.config.screens.ETFConfigScreenMain";
    private static final Size SIZE = new Size(24, 20);
    private static final Sprite UNFOCUSED = new Sprite(getEtfIcon("settings_unfocused.png"), SIZE);
    private static final Sprite FOCUSED = new Sprite(getEtfIcon("settings_focused.png"), SIZE);

    private ETFButtonFactory() {
    }

    public static SpriteButton<Void> create(Screen previous) {
        return Toggleable.applyPref(
                        Preferences.INSTANCE.etfButton,
                        SpriteButton.<Void>builder(SpriteButton.Sprites.of(new ButtonSprites(FOCUSED, UNFOCUSED)))
                )
                .setMessage(Mod.ETF.getId())
                .setDimensions(SIZE.width(), SIZE.height())
                .setOnPress(ModScreenFactory.createScreenSetter(
                        ETF_SCREEN_NAME,
                        new ModScreenFactory.Arg<>(Screen.class, previous)
                ))
                .build();
    }

    private static ResourceLocation getEtfIcon(String icon) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, "textures/gui/").withSuffix(icon);
    }
}
