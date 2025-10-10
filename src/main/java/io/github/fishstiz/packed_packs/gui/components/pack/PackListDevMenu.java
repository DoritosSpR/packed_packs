package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.transform.interfaces.ConfiguredPack;
import io.github.fishstiz.packed_packs.transform.interfaces.IPack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.util.lang.ObjectsUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;

import static io.github.fishstiz.packed_packs.gui.metadata.Toggleable.getDefaultIcon;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.devItem;
import static io.github.fishstiz.packed_packs.util.lang.ObjectsUtil.pick;

public record PackListDevMenu(PackListBase<?> list, Pack pack) {
    private static final int DEV_SPRITE_SIZE = 16;
    private static final int DEV_SPRITE_MARGIN_RIGHT = 8;
    private static final Sprite EYE_SLASH_SPRITE = Sprite.of16(ResourceUtil.getIcon("eye_slash"));
    private static final Sprite X_SQUARE = Sprite.of16(ResourceUtil.getIcon("x_square"));
    private static final Sprite ARROW_UP_SPRITE = Sprite.of16(ResourceUtil.getIcon("arrow_up"));
    private static final Sprite ARROW_DOWN_SPRITE = Sprite.of16(ResourceUtil.getIcon("arrow_down"));
    private static final Sprite ARROWS_SPRITE = Sprite.of16(ResourceUtil.getIcon("arrows_vertical"));
    private static final Sprite RADIO_GLOBAL = Sprite.of16(ResourceUtil.getIcon("radio_globe"));
    private static final Component HIDDEN = overrideText("hidden");
    private static final Component REQUIRED = overrideText("required");
    private static final Component FIXED_POSITION = overrideText("fixed");
    private static final Component FIXED_TOP = overrideText("fixed.top");
    private static final Component FIXED_BOTTOM = overrideText("fixed.bottom");
    private static final Component REMOVE_OVERRIDES = overrideText("remove");
    private static final Tooltip REQUIRED_NO_DISABLED_INFO = Tooltip.create(overrideText("required.no.disabled.info"));

    private static Component overrideText(String keySuffix) {
        return ResourceUtil.getText("profile.override." + keySuffix);
    }

    private PackOverrideScope hasOverride(BiPredicate<Profile, Pack> option) {
        Profile defaultProfile = this.list.packAssets.getConfig().getDefaultProfile();
        Profile currentProfile = this.list.packAssets.getProfile();
        PackOverrideScope packOverride = PackOverrideScope.NONE;

        if (defaultProfile != null && option.test(defaultProfile, this.pack)) {
            packOverride = PackOverrideScope.GLOBAL;
        }
        if (currentProfile != null && option.test(currentProfile, this.pack)) {
            packOverride = !packOverride.booleanValue() ? PackOverrideScope.LOCAL : PackOverrideScope.COMPOSITE;
        }
        return packOverride;
    }

    public void renderDevSprites(GuiGraphics guiGraphics, int top, int left, int width) {
        int size = DEV_SPRITE_SIZE;
        int iconX = (left + width) - size - DEV_SPRITE_MARGIN_RIGHT;

        PackOverrideScope positionOverride = this.hasOverride(Profile::overridesPosition);
        if (positionOverride.booleanValue()) {
            boolean unfixed = !this.list.packAssets.isFixed(this.pack);
            boolean fixedTop = this.list.packAssets.getPosition(this.pack) == Pack.Position.TOP;
            guiGraphics.fill(iconX, top, iconX + size, top + size, positionOverride.backgroundColor());
            pick(unfixed, ARROWS_SPRITE, pick(fixedTop, ARROW_UP_SPRITE, ARROW_DOWN_SPRITE)).render(guiGraphics, iconX, top, size, size);
            iconX -= size;
        }
        PackOverrideScope requiredOverride = this.hasOverride(Profile::overridesRequired);
        if (requiredOverride.booleanValue()) {
            boolean required = this.list.packAssets.isRequired(this.pack);
            guiGraphics.fill(iconX, top, iconX + size, top + size, requiredOverride.backgroundColor());
            pick(required, LOCK_SPRITE_SMALL, UNLOCK_SPRITE_SMALL).render(guiGraphics, iconX, top, size, size);
            iconX -= size;
        }
        PackOverrideScope hiddenOverride = this.hasOverride(Profile::isHidden);
        if (hiddenOverride.booleanValue()) {
            guiGraphics.fill(iconX, top, iconX + size, top + size, hiddenOverride.backgroundColor());
            EYE_SLASH_SPRITE.render(guiGraphics, iconX, top, size, size);
            iconX -= size;
        }
        PackOverrideScope included = this.hasOverride(Profile::includes);
        if (included.global() && !((ConfiguredPack) this.pack).packed_packs$getMetadata().compatibility().isCompatible()) {
            guiGraphics.fill(iconX, top, iconX + size, top + size, Theme.RED_700.withAlpha(0.75f));
            X_SQUARE.render(guiGraphics, iconX, top, size, size);
        }
    }

    private Pack[] selectionOrPack() {
        if (ObjectsUtil.testNullable(this.list.getEntry(this.pack), PackListBase.Entry::isSelected)) {
            return this.list.packAssets.flattenPacks(this.list.copySelection()).toArray(Pack[]::new);
        }
        return new Pack[]{this.pack};
    }

    private void updateRequired(@Nullable Boolean required) {
        Profile profile = this.list.packAssets.getProfile();
        if (profile != null) {
            profile.setRequired(required, this.selectionOrPack());
            if (Boolean.TRUE.equals(required) && this.list instanceof AvailablePackList) {
                ObjectsUtil.mapOrNull(this.list.getEntry(this.pack), PackListBase.Entry::transfer);
            }
        }
    }

    private void updatePosition(@Nullable PackOverride.Position position) {
        Profile profile = this.list.packAssets.getProfile();
        if (profile != null) {
            profile.setPosition(position, this.selectionOrPack());
        }
    }

    private void resetOverrides() {
        this.updateHidden(false);
        this.updateRequired(null);
        this.updatePosition(null);
    }

    private void updateHidden(boolean hidden) {
        Profile profile = this.list.packAssets.getProfile();
        if (profile != null) {
            profile.setHidden(hidden, this.selectionOrPack());
        }
    }

    private Sprite getIcon(boolean active, BiPredicate<Profile, Pack> defaultOption) {
        return this.hasOverride(defaultOption) == PackOverrideScope.GLOBAL ? RADIO_GLOBAL : getDefaultIcon(active);
    }

    private boolean canDisableRequired() {
        Profile profile = this.list.packAssets.getProfile();
        return profile != null && profile == this.list.packAssets.getConfig().getDefaultProfile() && !PackUtil.isEssential(this.pack);
    }

    public void onBuildHeader(ContextMenuItemBuilder builder) {
        Profile profile = this.list.packAssets.getProfile();
        if (profile == null) return;

        builder.add(devItem(HIDDEN)
                .icon(() -> this.getIcon(profile.isHidden(this.pack), Profile::isHidden))
                .activeWhen(() -> this.hasOverride(Profile::isHidden) != PackOverrideScope.GLOBAL)
                .action(() -> this.updateHidden(!profile.isHidden(this.pack)))
                .closeOnInteract(false)
                .build());

        builder.add(devItem(REQUIRED)
                .icon(() -> this.getIcon(profile.overridesRequired(this.pack), Profile::overridesRequired))
                .activeWhen(() -> this.hasOverride(Profile::overridesRequired) != PackOverrideScope.GLOBAL &&
                                  !((IPack) this.pack).packed_packs$nestedPack())
                .closeOnInteract(false)
                .addChild(devItem(CommonComponents.OPTION_OFF)
                        .icon(() -> getDefaultIcon(!profile.overridesRequired(this.pack)))
                        .action(() -> this.updateRequired(null))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(CommonComponents.GUI_NO)
                        .icon(() -> getDefaultIcon(profile.overridesRequired(this.pack) && !profile.isRequired(this.pack)))
                        .activeWhen(this::canDisableRequired)
                        .tooltip(() -> !this.canDisableRequired() && !PackUtil.isEssential(pack) ? REQUIRED_NO_DISABLED_INFO : null)
                        .action(() -> this.updateRequired(false))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(CommonComponents.GUI_YES)
                        .icon(() -> getDefaultIcon(profile.isRequired(this.pack)))
                        .action(() -> this.updateRequired(true))
                        .closeOnInteract(false)
                        .build())
                .build());

        builder.add(devItem(FIXED_POSITION)
                .icon(() -> this.getIcon(profile.overridesPosition(this.pack), Profile::overridesPosition))
                .activeWhen(() -> this.hasOverride(Profile::overridesPosition) != PackOverrideScope.GLOBAL)
                .closeOnInteract(false)
                .addChild(devItem(CommonComponents.OPTION_OFF)
                        .icon(() -> getDefaultIcon(!profile.overridesPosition(this.pack)))
                        .action(() -> this.updatePosition(null))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(CommonComponents.GUI_NO)
                        .icon(() -> getDefaultIcon(profile.overridesPosition(this.pack) && !profile.isFixed(this.pack)))
                        .action(() -> this.updatePosition(PackOverride.Position.UNFIXED))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(FIXED_TOP)
                        .icon(() -> getDefaultIcon(profile.getPositionOverride(this.pack) == PackOverride.Position.TOP))
                        .action(() -> this.updatePosition(PackOverride.Position.TOP))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(FIXED_BOTTOM)
                        .icon(() -> getDefaultIcon(profile.getPositionOverride(this.pack) == PackOverride.Position.BOTTOM))
                        .action(() -> this.updatePosition(PackOverride.Position.BOTTOM))
                        .closeOnInteract(false)
                        .build())
                .build());

        builder.add(devItem(REMOVE_OVERRIDES).action(this::resetOverrides).build()).separator();
    }

    enum PackOverrideScope {
        NONE(Theme.WHITE.withAlpha(0)),
        LOCAL(Theme.BLACK.withAlpha(0.75f)),
        GLOBAL(Theme.BLUE_500.withAlpha(0.75f)),
        COMPOSITE(Theme.PURPLE_500.withAlpha(0.75f));

        private final int backgroundColor;

        PackOverrideScope(int backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        boolean booleanValue() {
            return this != NONE;
        }

        boolean global() {
            return this == GLOBAL || this == COMPOSITE;
        }

        int backgroundColor() {
            return this.backgroundColor;
        }
    }
}
