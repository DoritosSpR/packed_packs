package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.components.profile.ProfileList;
import io.github.fishstiz.packed_packs.gui.components.profile.Sidebar;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;

public class ProfilesLayout {
    public static final Component TITLE_TEXT = ResourceUtil.getText("profile");
    private static final Component EDIT_NAME_TEXT = ResourceUtil.getText("profile.edit");
    private static final Component NO_PROFILE_TEXT = ResourceUtil.getText("profile.none");
    private static final Component UNNAMED_TEXT = ResourceUtil.getText("profile.unnamed");
    private static final Component NEW_TEXT = ResourceUtil.getText("profile.new");
    private static final Component NEW_INFO = ResourceUtil.getText("profile.new.info");
    private static final Component COPY_TEXT = ResourceUtil.getText("profile.copy");
    private static final int MAX_WIDTH = SPACING * 20;
    private final Config.Packs config;
    private final Sidebar sidebar;
    private final PackedPacksScreen screen;
    private final ToggleableEditBox<Void> nameField;
    private final FidgetzButton<Void> toggleNameButton;
    private final FidgetzButton<Void> noProfileButton;
    private final ProfileList profileList;
    private @Nullable Profile profile;

    public ProfilesLayout(@Nullable Profile profile, Config.Packs config, PackedPacksScreen screen) {
        this.config = config;
        this.screen = screen;
        this.sidebar = Sidebar.builder(screen)
                .setHeaderSettings(LayoutSettings.defaults().paddingLeft(SPACING).paddingTop(SPACING - 1))
                .setMaxWidth(MAX_WIDTH)
                .setTitle(TITLE_TEXT, true)
                .build();
        this.nameField = ToggleableEditBox.<Void>builder()
                .setHint(UNNAMED_TEXT)
                .setMaxLength(Profile.NAME_MAX_LENGTH)
                .setFilter(value -> value != null && (value.isEmpty() || !value.isBlank()))
                .addListener(this::onNameChange)
                .build();
        this.toggleNameButton = FidgetzButton.<Void>builder()
                .makeSquare()
                .setTooltip(Tooltip.create(EDIT_NAME_TEXT))
                .setSprite(new ButtonSprites(
                        new Sprite(ResourceUtil.getIcon("edit"), Size.of16()),
                        new Sprite(ResourceUtil.getIcon("edit_inactive"), Size.of16()),
                        this::getToggleSpriteRenderer
                ))
                .setOnPress(this.nameField::toggle)
                .build();
        this.noProfileButton = FidgetzButton.<Void>builder()
                .setMessage(NO_PROFILE_TEXT)
                .setOnPress(() -> this.setProfile(null))
                .build();
        this.profileList = new ProfileList(this.config, this);
        this.profile = profile;
    }

    public void initContents() {
        LayoutSettings layoutSettings = LayoutSettings.defaults().paddingHorizontal(GuiConstants.SPACING);
        FlexLayout actions = FlexLayout.horizontal(() -> MAX_WIDTH).spacing(GuiConstants.SPACING);
        FlexLayout list = FlexLayout.horizontal(() -> MAX_WIDTH);

        actions.addFlexChild(this.noProfileButton);
        actions.addFlexChild(
                FidgetzButton.<Void>builder()
                        .setMessage(NEW_TEXT)
                        .setTooltip(Tooltip.create(NEW_INFO))
                        .setOnPress(this::copyProfile)
                        .build()
        );
        list.addFlexChild(this.profileList, true);

        this.sidebar.root().layout().addChild(actions, layoutSettings);
        this.sidebar.root().layout().addFlexChild(list, true, layoutSettings.copy().paddingBottom(GuiConstants.SPACING + 1));
        this.sidebar.root().layout().arrangeElements();
        this.sidebar.root().layout().visitWidgets(this.sidebar::addRenderableWidget);

        this.updateGuiState(this.profile);
    }

    private RenderableRect getToggleSpriteRenderer(Sprite sprite) {
        if (this.profile != null && this.profile.isLocked()) {
            Profile defaultProfile = this.config.getDefaultProfile();
            return this.profile == defaultProfile ? STAR_SPRITE::renderClamped : LOCK_SPRITE;
        }
        return sprite::renderClamped;
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
        if (this.profile != null) {
            String name = value;

            if (value.isEmpty()) {
                name = UNNAMED_TEXT.getString();
            }

            this.profile.setName(name);
        }
        this.profileList.scheduleRefresh();
    }

    public void updateGuiState(@Nullable Profile profile) {
        this.nameField.setEditable(false);

        boolean hasProfile = profile != null;
        this.nameField.setHint(hasProfile ? UNNAMED_TEXT : NO_PROFILE_TEXT);
        this.nameField.setValue(hasProfile ? profile.getName() : "");
        this.nameField.visible = hasProfile;
        this.nameField.active = hasProfile;
        this.noProfileButton.active = hasProfile;
        this.toggleNameButton.visible = hasProfile;
        this.toggleNameButton.active = hasProfile && !profile.isLocked();
    }

    public void setProfile(@Nullable Profile profile) {
        Profile previous = this.profile;
        this.profile = profile;
        this.screen.onProfileChange(previous, this.profile);
        this.updateGuiState(profile);
    }

    public @Nullable Profile getProfile() {
        return this.profile;
    }

    private void copyProfile() {
        Profile copiedProfile = this.profile != null
                ? this.profile.copy()
                : new Profile(NO_PROFILE_TEXT.getString() + " - " + COPY_TEXT.getString());

        this.screen.onProfileCopy(this.profile, copiedProfile);
        this.config.addProfile(copiedProfile);
        this.setProfile(copiedProfile);
        this.sidebar.setOpen(false);
        this.profileList.refresh();
    }

    public void removeProfile(Profile profile) {
        if (profile != null && this.profile == profile) {
            List<Profile> profiles = this.config.getProfiles();
            if (!profiles.isEmpty()) {
                int index = profiles.indexOf(profile);
                Profile previous = (index > 0) ? profiles.get(index - 1) : null;
                this.setProfile(previous);
            } else {
                this.setProfile(null);
            }
        }
        this.config.removeProfile(profile);
        this.profileList.refresh();
    }
}
