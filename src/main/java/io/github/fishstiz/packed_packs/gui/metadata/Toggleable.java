package io.github.fishstiz.packed_packs.gui.metadata;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItem;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.MenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.ModAdditions;
import io.github.fishstiz.packed_packs.config.Preferences;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public record Toggleable(
        Consumer<Boolean> toggler,
        Supplier<Boolean> toggled,
        Function<Boolean, Component> text,
        @Nullable ToIntFunction<Boolean> foreground,
        @Nullable ToIntFunction<Boolean> border,
        @Nullable Function<Boolean, Sprite> icon
) implements RenderableRect {
    public Toggleable(Consumer<Boolean> toggler, Supplier<Boolean> toggled, Function<Boolean, Component> text) {
        this(toggler, toggled, text, Toggleable::getDefaultForeground, Toggleable::getDefaultBorder, Toggleable::getDefaultIcon);
    }

    public Toggleable(Preferences.Preference<Boolean> pref) {
        this(pref::set, pref::get, enabled -> ResourceUtil.getText("preferences.widgets." + pref.getKey()));
    }

    public void toggle() {
        this.toggler.accept(!this.toggled.get());
    }

    public void buildContext(ContextMenuItemBuilder builder) {
        builder.add(itemBuilder().build());
    }

    public MenuItemBuilder itemBuilder() {
        return MenuItem.builder(this.text.apply(this.toggled.get()))
                .background(GuiConstants.DEVELOPER_MODE_ITEM_BACKGROUND)
                .icon(() -> this.icon != null ? this.icon.apply(this.toggled.get()) : null)
                .action(this::toggle);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
        final boolean toggled = this.toggled.get();

        if (this.foreground != null) {
            guiGraphics.fill(x, y, x + width, y + height, this.foreground.applyAsInt(toggled));
        }

        if (this.border != null) {
            DrawUtil.renderOutline(guiGraphics, x, y, width, height, this.border.applyAsInt(toggled));
        }
    }

    public static int getDefaultForeground(boolean enabled) {
        return enabled ? Theme.GREEN_500.withAlpha(0.5f) : Theme.RED_700.withAlpha(0.5f);
    }

    public static int getDefaultBorder(boolean enabled) {
        return enabled ? Theme.GREEN_500.getARGB() : Theme.RED_700.getARGB();
    }

    public static Sprite getDefaultIcon(boolean enabled) {
        return enabled ? GuiConstants.RADIO_ON_SPRITE : GuiConstants.RADIO_OFF_SPRITE;
    }

    public static MenuItem fromPref(Preferences.Preference<Boolean> pref) {
        return new Toggleable(pref)
                .itemBuilder()
                .closeOnInteract(false)
                .build();
    }

    public static <T extends FidgetzButton.Builder<?, ?>> T applyPref(Preferences.Preference<Boolean> pref, T builder) {
        if (PackedPacks.CONFIG.isDevMode()) {
            Toggleable toggleablePref = new Toggleable(pref);
            builder.setForeground(toggleablePref).setContextMenuBuilder((btn, b) -> toggleablePref.buildContext(b.separatorIfNonEmpty()));
        }
        return builder;
    }

    public static List<MenuItem> preferences(PackType packType) {
        Preferences prefs = Preferences.INSTANCE;
        ContextMenuItemBuilder builder = new ContextMenuItemBuilder();

        builder.add(fromPref(prefs.originalScreenWidget));
        builder.add(fromPref(prefs.optionsWidget));
        builder.add(fromPref(prefs.actionBarWidget));
        builder.add(fromPref(prefs.toggleIncompatibleWidget));
        builder.add(fromPref(prefs.folderPackWidget));

        ModAdditions.onCreatePreferencesMenu(packType, builder);

        return builder.build();
    }
}
