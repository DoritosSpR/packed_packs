package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.components.profile.ProfileList;
import io.github.fishstiz.packed_packs.gui.components.profile.Sidebar;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProfilesLayout {
    public static final Component TITLE_TEXT = ResourceUtil.getText("profile");
    private static final Component EDIT_NAME_TEXT = ResourceUtil.getText("profile.edit");
    private static final Component NO_PROFILE_TEXT = ResourceUtil.getText("profile.none");
    private static final Component UNNAMED_TEXT = ResourceUtil.getText("profile.unnamed");
    private static final Component NEW_TEXT = ResourceUtil.getText("profile.new");
    private static final Component NEW_INFO = ResourceUtil.getText("profile.new.info");
    private static final Component COPY_TEXT = ResourceUtil.getText("profile.copy");
    private static final int MAX_WIDTH = 150;
    private final Config.Packs config;
    private final Sidebar sidebar;
    private final Listener listener;
    private final ToggleableEditBox<Void> nameField = ToggleableEditBox.<Void>builder()
            .setHint(UNNAMED_TEXT)
            .setMaxLength(Profile.NAME_MAX_LENGTH)
            .setFilter(value -> value != null && (value.isEmpty() || !value.isBlank()))
            .addListener(this::onNameChange)
            .build();
    private final FidgetzButton<Void> toggleNameButton = FidgetzButton.<Void>builder()
            .makeSquare()
            .setTooltip(Tooltip.create(EDIT_NAME_TEXT))
            .setSprite(new ButtonSprites(
                    new Sprite(ResourceUtil.getIcon("edit"), Size.of16()),
                    new Sprite(ResourceUtil.getIcon("edit_inactive"), Size.of16()),
                    sprite -> this.getProfile() != null && this.getProfile().isLocked()
                            ? GuiConstants.LOCK_SPRITE
                            : sprite::renderClamped
            ))
            .setOnPress(nameField::toggle)
            .build();
    private final FidgetzButton<Void> noProfileButton = FidgetzButton.<Void>builder()
            .setMessage(NO_PROFILE_TEXT)
            .setOnPress(() -> this.setProfile(null))
            .build();
    private final ProfileList profileList;
    private @Nullable Profile profile;

    public ProfilesLayout(Sidebar.Builder sidebar, Config.Packs config, Listener listener) {
        this.config = config;
        this.sidebar = sidebar.setMaxWidth(MAX_WIDTH).setTitle(TITLE_TEXT.copy().withColor(Theme.GRAY_800.getARGB()), false).build();
        this.listener = listener;
        this.profileList = new ProfileList(this.config, this::getProfile, this::removeProfile, this::setProfile);

        this.noProfileButton.addListener(() -> this.sidebar.setOpen(false));
    }

    public void initContents() {
        LayoutSettings layoutSettings = LayoutSettings.defaults().paddingHorizontal(GuiConstants.SPACING);
        FlexLayout actions = FlexLayout.horizontal(this::getMaxWidth).spacing(GuiConstants.SPACING);
        FlexLayout list = FlexLayout.horizontal(this::getMaxWidth);

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

        this.setProfile(this.config.getLastViewed());
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

    private void setProfile(@Nullable Profile profile) {
        Profile previous = this.profile;
        this.profile = profile;

        this.nameField.setEditable(false);

        boolean hasProfile = profile != null;
        this.nameField.setHint(hasProfile ? UNNAMED_TEXT : NO_PROFILE_TEXT);
        this.nameField.setValue(hasProfile ? profile.getName() : "");
        this.toggleNameButton.active = hasProfile && !profile.isLocked();
        this.noProfileButton.active = hasProfile;

        this.listener.onProfileChange(previous, this.profile);
    }

    public @Nullable Profile getProfile() {
        return this.profile;
    }

    private void copyProfile() {
        Profile copiedProfile = this.profile != null
                ? this.profile.copy()
                : new Profile(NO_PROFILE_TEXT.getString() + " - " + COPY_TEXT.getString());

        this.listener.onProfileCopy(this.profile, copiedProfile);
        this.config.addProfile(copiedProfile);
        this.setProfile(copiedProfile);
        this.sidebar.setOpen(false);
        this.profileList.refresh();
    }

    private void removeProfile(Profile profile) {
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

    public interface Listener {
        void onProfileChange(@Nullable Profile previous, @Nullable Profile current);

        void onProfileCopy(@Nullable Profile original, @NotNull Profile copy);
    }
}
