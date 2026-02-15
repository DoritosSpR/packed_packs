package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleableDialogContainer;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.config.Profiles;
import io.github.fishstiz.packed_packs.gui.components.profile.ProfileList;
import io.github.fishstiz.packed_packs.gui.components.profile.Sidebar;
import io.github.fishstiz.packed_packs.gui.screens.WidgetFactory;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;

public class ProfilesLayout {
    public static final Component TITLE_TEXT = ResourceUtil.getText("profile");
    private static final Component NO_PROFILE_TEXT = ResourceUtil.getText("profile.none");
    private static final Component UNNAMED_TEXT = ResourceUtil.getText("profile.unnamed");
    private static final Component COPY_TEXT = ResourceUtil.getText("profile.copy");
    private static final int MAX_WIDTH = SPACING * 20;
    private final Config.Packs userConfig;
    private final DevConfig.Packs config;
    private final BiConsumer<Profile, Profile> copyListener;
    private final ButtonSprites toggleSprites;
    private final Sidebar sidebar;
    private final ProfileList profileList;
    private ToggleableEditBox<Void> nameField;
    private FidgetzButton<Void> toggleNameButton;
    private FidgetzButton<Void> noProfileButton;
    private boolean initialized = false;

    public <S extends Screen & ToggleableDialogContainer> ProfilesLayout(
            S screen,
            Config.Packs userConfig,
            DevConfig.Packs config,
            BiConsumer<Profile, Profile> selectListener,
            BiConsumer<Profile, Profile> copyListener
    ) {
        this.userConfig = userConfig;
        this.config = config;
        this.copyListener = copyListener;
        this.toggleSprites = new ButtonSprites(
                Sprite.of16(ResourceUtil.getIcon("edit")),
                Sprite.of16(ResourceUtil.getIcon("edit_inactive"))
        );

        WidgetFactory.ProfileWidgets widgets = WidgetFactory.createProfileWidgets(screen, userConfig, config, selectListener, this::updateGuiState);
        this.sidebar = widgets.sidebar();
        this.profileList = widgets.profileList();
    }

    public void init(Runnable onClose) {
        this.nameField = ToggleableEditBox.<Void>builder()
                .setHint(UNNAMED_TEXT)
                .setMaxLength(Profile.NAME_MAX_LENGTH)
                .setFilter(value -> value != null && (value.isEmpty() || !value.isBlank()))
                .addListener(this::onNameChange)
                .build();
        this.toggleNameButton = FidgetzButton.<Void>builder()
                .makeSquare()
                .setTooltip(Tooltip.create(ResourceUtil.getText("profile.edit")))
                .setSprite(this.toggleSprites)
                .setOnPress(this.nameField::toggle)
                .build();

        final FidgetzButton<Void> copyButton = FidgetzButton.<Void>builder()
                .setMessage(ResourceUtil.getText("profile.new"))
                .setTooltip(Tooltip.create(ResourceUtil.getText("profile.new.info")))
                .setOnPress(this::copyProfile)
                .build();
        this.noProfileButton = FidgetzButton.<Void>builder()
                .setMessage(NO_PROFILE_TEXT)
                .setOnPress(() -> this.setProfile(null))
                .build();

        final FlexLayout actions = FlexLayout.horizontal(this::getMaxWidth).spacing(GuiConstants.SPACING);
        actions.addFlexChild(this.noProfileButton);
        actions.addFlexChild(copyButton);

        final FlexLayout list = FlexLayout.horizontal(this::getMaxWidth);
        list.addFlexChild(this.profileList, true);

        this.sidebar.init(TITLE_TEXT, onClose, MAX_WIDTH);
        this.sidebar.root().layout().addChild(actions);
        this.sidebar.root().layout().addFlexChild(list, true);
        this.sidebar.root().layout().visitWidgets(this.sidebar::addRenderableWidget);

        this.initialized = true;

        this.updateGuiState(this.profileList.getSelectedProfile());
        this.profileList.refresh();
    }

    public int getMaxWidth() {
        return MAX_WIDTH;
    }

    public Sidebar getSidebar() {
        return this.sidebar;
    }

    public ToggleableEditBox<Void> getNameField() {
        return this.nameField;
    }

    public FidgetzButton<Void> getToggleNameButton() {
        return this.toggleNameButton;
    }

    private void onNameChange(String value) {
        Profile profile = this.profileList.getSelectedProfile();
        if (profile != null) {
            String name = value;

            if (value.isEmpty()) {
                name = UNNAMED_TEXT.getString();
            }

            this.userConfig.renameProfile(profile, name);
        }
        this.profileList.scheduleRefresh();
    }

    private void updateGuiState(@Nullable Profile profile) {
        if (!this.initialized) return;

        this.nameField.setEditable(false);

        boolean hasProfile = profile != null;
        this.nameField.setHint(hasProfile ? UNNAMED_TEXT : NO_PROFILE_TEXT);
        this.nameField.setValueSilently(hasProfile ? profile.getName() : "");
        this.nameField.visible = hasProfile;
        this.nameField.active = hasProfile;
        this.noProfileButton.active = hasProfile;
        this.toggleNameButton.visible = hasProfile;
        this.toggleNameButton.active = hasProfile && !profile.isLocked();

        if (hasProfile && profile.isLocked()) {
            Profile defaultProfile = this.config.getDefaultProfile();
            ButtonSprites sprites = profile == defaultProfile ? ButtonSprites.of(STAR_SPRITE) : ButtonSprites.unclamp(LOCK_SPRITE);
            this.toggleNameButton.setSprites(sprites);
        } else {
            this.toggleNameButton.setSprites(this.toggleSprites);
        }
    }

    public void setProfile(@Nullable Profile profile) {
        this.profileList.selectProfile(profile);
    }

    public @Nullable Profile getProfile() {
        return this.profileList.getSelectedProfile();
    }

    private void copyProfile() {
        Profile selectedProfile = this.profileList.getSelectedProfile();
        Profile copiedProfile;

        if (selectedProfile != null) {
            if (selectedProfile.isTemp()) {
                Profiles.save(this.userConfig.packType(), selectedProfile);
            }
            copiedProfile = selectedProfile.copy();
        } else {
            copiedProfile = Profiles.create(NO_PROFILE_TEXT.getString() + " - " + COPY_TEXT.getString(), this.userConfig.packType());
        }

        this.copyListener.accept(selectedProfile, copiedProfile);
        this.userConfig.addProfile(copiedProfile);
        this.setProfile(copiedProfile);
        this.sidebar.setOpen(false);
        this.profileList.refresh();
    }
}
