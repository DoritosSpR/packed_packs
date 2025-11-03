package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.components.SelectionContext;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.packed_packs.pack.ProfileScope;
import io.github.fishstiz.packed_packs.transform.interfaces.ConfiguredPack;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.gui.components.ToggleableHelper.getDefaultIcon;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.devItem;
import static io.github.fishstiz.fidgetz.util.lang.ObjectsUtil.pick;

public record PackListDevMenu(
        Minecraft minecraft,
        PackOptionsContext options,
        SelectionContext<Pack> context,
        Consumer<Event<?>> listener
) {
    private static final int DEV_SPRITE_SIZE = 16;
    private static final int DEV_SPRITE_MARGIN_RIGHT = 8;
    private static final Sprite EYE_SLASH_SPRITE = Sprite.of16(ResourceUtil.getIcon("eye_slash"));
    private static final Sprite X_SQUARE = Sprite.of16(ResourceUtil.getIcon("x_square"));
    private static final Sprite ARROW_UP_SPRITE = Sprite.of16(ResourceUtil.getIcon("arrow_up"));
    private static final Sprite ARROW_DOWN_SPRITE = Sprite.of16(ResourceUtil.getIcon("arrow_down"));
    private static final Sprite ARROWS_SPRITE = Sprite.of16(ResourceUtil.getIcon("arrows_vertical"));
    private static final Sprite RADIO_GLOBAL = Sprite.of16(ResourceUtil.getIcon("radio_globe"));
    private static final Sprite ALIAS_SPRITE = Sprite.of16(ResourceUtil.getIcon("alias"));
    private static final Component HIDDEN = overrideText("hidden");
    private static final Component REQUIRED = overrideText("required");
    private static final Component FIXED_POSITION = overrideText("fixed");
    private static final Component FIXED_TOP = overrideText("fixed.top");
    private static final Component FIXED_BOTTOM = overrideText("fixed.bottom");
    private static final Component REMOVE_OVERRIDES = overrideText("remove");
    private static final Tooltip REQUIRED_NO_DISABLED_INFO = Tooltip.create(overrideText("required.no.disabled.info"));

    public sealed interface Event<T> {
        Pack trigger();

        List<Pack> packs();

        T value();

        record EditAliases(Pack trigger, Boolean value) implements Event<Boolean> {
            public List<Pack> packs() {
                return List.of(this.trigger);
            }
        }

        record Hide(Pack trigger, Boolean value, List<Pack> packs) implements Event<Boolean> {
        }

        record Require(Pack trigger, @Nullable Boolean value, List<Pack> packs) implements Event<Boolean> {
        }

        record Reposition(
                Pack trigger,
                @Nullable PackOverride.Position value,
                List<Pack> packs
        ) implements Event<PackOverride.Position> {
        }
    }

    @FunctionalInterface
    private interface EventFactory<T> {
        Event<T> create(Pack pack, T value, List<Pack> packs);
    }

    private <T> void notifyListener(T value, List<Pack> packs, EventFactory<T> eventFactory) {
        if (this.listener != null) {
            this.listener.accept(eventFactory.create(this.pack(), value, packs));
        }
    }

    private static Component overrideText(String keySuffix) {
        return ResourceUtil.getText("profile.override." + keySuffix);
    }

    private Pack pack() {
        return this.context.item();
    }

    private ProfileScope hasOverride(BiPredicate<Profile, Pack> option) {
        return this.options.hasOverride(this.context.item(), option);
    }

    private List<Pack> getPackOrSelection() {
        return this.context.getItemOrSelection();
    }

    public void renderDevSprites(GuiGraphics guiGraphics, int top, int left, int width) {
        int size = DEV_SPRITE_SIZE;
        int iconX = (left + width) - size - DEV_SPRITE_MARGIN_RIGHT;

        ProfileScope positionOverride = this.hasOverride(Profile::overridesPosition);
        if (positionOverride.exists()) {
            boolean unfixed = !this.options.isFixed(this.pack());
            boolean fixedTop = this.options.getPosition(this.pack()) == Pack.Position.TOP;
            guiGraphics.fill(iconX, top, iconX + size, top + size, getBackgroundColor(positionOverride));
            pick(unfixed, ARROWS_SPRITE, pick(fixedTop, ARROW_UP_SPRITE, ARROW_DOWN_SPRITE)).render(guiGraphics, iconX, top, size, size);
            iconX -= size;
        }
        ProfileScope requiredOverride = this.hasOverride(Profile::overridesRequired);
        if (requiredOverride.exists()) {
            boolean required = this.options.isRequired(this.pack());
            guiGraphics.fill(iconX, top, iconX + size, top + size, getBackgroundColor(requiredOverride));
            pick(required, LOCK_SPRITE_SMALL, UNLOCK_SPRITE_SMALL).render(guiGraphics, iconX, top, size, size);
            iconX -= size;
        }
        ProfileScope hiddenOverride = this.hasOverride(Profile::isHidden);
        if (hiddenOverride.exists()) {
            guiGraphics.fill(iconX, top, iconX + size, top + size, getBackgroundColor(hiddenOverride));
            EYE_SLASH_SPRITE.render(guiGraphics, iconX, top, size, size);
            iconX -= size;
        }
        ProfileScope included = this.hasOverride(Profile::includes);
        if (included.global() && !((ConfiguredPack) this.pack()).packed_packs$getMetadata().compatibility().isCompatible()) {
            guiGraphics.fill(iconX, top, iconX + size, top + size, Theme.RED_700.withAlpha(0.75f));
            X_SQUARE.render(guiGraphics, iconX, top, size, size);
            iconX -= size;
        }
        if (this.options.getConfig().hasAlias(this.pack().getId())) {
            guiGraphics.fill(iconX, top, iconX + size, top + size, Theme.GREEN_500.withAlpha(0.75f));
            ALIAS_SPRITE.render(guiGraphics, iconX, top, size, size);
        }
    }

    private void updateHidden(boolean hidden) {
        this.options.getProfile().ifPresent(profile -> {
            List<Pack> selected = this.getPackOrSelection();
            profile.setHidden(hidden, selected);
            this.notifyListener(hidden, selected, Event.Hide::new);
        });
    }

    private void updateRequired(@Nullable Boolean required) {
        this.options.getProfile().ifPresent(profile -> {
            List<Pack> selected = this.getPackOrSelection();
            profile.setRequired(required, selected);
            this.notifyListener(required, selected, Event.Require::new);
        });
    }

    private void updatePosition(@Nullable PackOverride.Position position) {
        this.options.getProfile().ifPresent(profile -> {
            List<Pack> selected = this.getPackOrSelection();
            profile.setPosition(position, selected);
            this.notifyListener(position, selected, Event.Reposition::new);
        });
    }

    private void resetOverrides() {
        this.updateHidden(false);
        this.updateRequired(null);
        this.updatePosition(null);
    }

    private Sprite getIcon(boolean active, BiPredicate<Profile, Pack> defaultOption) {
        return this.hasOverride(defaultOption) == ProfileScope.GLOBAL ? RADIO_GLOBAL : getDefaultIcon(active);
    }

    private boolean canDisableRequired() {
        return this.options.isDefaultProfile() && !PackUtil.isEssential(this.pack());
    }

    private void buildNonOverrideOptions(ContextMenuItemBuilder builder) {
        builder.add(devItem(Component.translatable("chat.copy"))
                .action(() -> this.minecraft.keyboardHandler.setClipboard(this.pack().getId()))
                .build()
        ).separator();

        builder.add(devItem(ResourceUtil.getText("aliases.edit"))
                .action(() -> this.listener.accept(new Event.EditAliases(this.pack(), this.options.getConfig().hasAlias(this.pack().getId()))))
                .build()
        ).separator();
    }

    public void onBuildHeader(ContextMenuItemBuilder builder) {
        Profile profile = this.options.getProfile().orElse(null);
        if (profile == null) {
            this.buildNonOverrideOptions(builder);
            return;
        }

        builder.add(devItem(HIDDEN)
                .icon(() -> this.getIcon(profile.isHidden(this.pack()), Profile::isHidden))
                .activeWhen(() -> this.hasOverride(Profile::isHidden) != ProfileScope.GLOBAL)
                .action(() -> this.updateHidden(!profile.isHidden(this.pack())))
                .closeOnInteract(false)
                .build());

        builder.add(devItem(REQUIRED)
                .icon(() -> this.options.isLocked()
                        ? LOCK_SPRITE_SMALL
                        : this.getIcon(profile.overridesRequired(this.pack()), Profile::overridesRequired))
                .activeWhen(() -> !this.options.isLocked() &&
                                  this.hasOverride(Profile::overridesRequired) != ProfileScope.GLOBAL &&
                                  !((FilePack) this.pack()).packed_packs$nestedPack())
                .closeOnInteract(false)
                .addChild(devItem(CommonComponents.OPTION_OFF)
                        .icon(() -> getDefaultIcon(!profile.overridesRequired(this.pack())))
                        .action(() -> this.updateRequired(null))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(CommonComponents.GUI_NO)
                        .icon(() -> getDefaultIcon(profile.overridesRequired(this.pack()) && !profile.isRequired(this.pack())))
                        .activeWhen(this::canDisableRequired)
                        .tooltip(() -> !this.canDisableRequired() && !PackUtil.isEssential(this.pack()) ? REQUIRED_NO_DISABLED_INFO : null)
                        .action(() -> this.updateRequired(false))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(CommonComponents.GUI_YES)
                        .icon(() -> getDefaultIcon(profile.isRequired(this.pack())))
                        .action(() -> this.updateRequired(true))
                        .closeOnInteract(false)
                        .build())
                .build());

        builder.add(devItem(FIXED_POSITION)
                .icon(() -> this.getIcon(profile.overridesPosition(this.pack()), Profile::overridesPosition))
                .activeWhen(() -> this.hasOverride(Profile::overridesPosition) != ProfileScope.GLOBAL)
                .closeOnInteract(false)
                .addChild(devItem(CommonComponents.OPTION_OFF)
                        .icon(() -> getDefaultIcon(!profile.overridesPosition(this.pack())))
                        .action(() -> this.updatePosition(null))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(CommonComponents.GUI_NO)
                        .icon(() -> getDefaultIcon(profile.overridesPosition(this.pack()) && !profile.isFixed(this.pack())))
                        .action(() -> this.updatePosition(PackOverride.Position.UNFIXED))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(FIXED_TOP)
                        .icon(() -> getDefaultIcon(profile.getPositionOverride(this.pack()) == PackOverride.Position.TOP))
                        .action(() -> this.updatePosition(PackOverride.Position.TOP))
                        .closeOnInteract(false)
                        .build())
                .addChild(devItem(FIXED_BOTTOM)
                        .icon(() -> getDefaultIcon(profile.getPositionOverride(this.pack()) == PackOverride.Position.BOTTOM))
                        .action(() -> this.updatePosition(PackOverride.Position.BOTTOM))
                        .closeOnInteract(false)
                        .build())
                .build());

        builder.add(devItem(REMOVE_OVERRIDES).action(this::resetOverrides).build()).separator();

        this.buildNonOverrideOptions(builder);
    }

    private static int getBackgroundColor(ProfileScope overrideScope) {
        return switch (overrideScope) {
            case NONE -> Theme.WHITE.withAlpha(0);
            case LOCAL -> Theme.BLACK.withAlpha(0.75f);
            case GLOBAL -> Theme.BLUE_500.withAlpha(0.75f);
            case COMPOSITE -> Theme.PURPLE_500.withAlpha(0.75f);
        };
    }
}
