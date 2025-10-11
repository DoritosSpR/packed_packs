package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleableDialogContainer;
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
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private final CopyListener copyListener;
    private final ToggleableEditBox<Void> nameField;
    private final FidgetzButton<Void> toggleNameButton;
    private final FidgetzButton<Void> noProfileButton;
    private final ProfileList profileList;

    public <S extends Screen & ToggleableDialogContainer> ProfilesLayout(
            S screen,
            Config.Packs config,
            ProfileList.SelectListener selectListener,
            CopyListener copyListener
    ) {
        this.config = config;
        this.copyListener = copyListener;
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
        this.profileList = new ProfileList(this.config, selectListener, this::updateGuiState);
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

        this.updateGuiState(this.profileList.getSelectedProfile());
    }

    private RenderableRect getToggleSpriteRenderer(Sprite sprite) {
        Profile profile = this.profileList.getSelectedProfile();
        if (profile != null && profile.isLocked()) {
            Profile defaultProfile = this.config.getDefaultProfile();
            return profile == defaultProfile ? STAR_SPRITE::renderClamped : LOCK_SPRITE;
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
        Profile profile = this.profileList.getSelectedProfile();
        if (profile != null) {
            String name = value;

            if (value.isEmpty()) {
                name = UNNAMED_TEXT.getString();
            }

            profile.setName(name);
        }
        this.profileList.scheduleRefresh();
    }

    private void updateGuiState(@Nullable Profile profile) {
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
        this.profileList.selectProfile(profile);
    }

    public @Nullable Profile getProfile() {
        return this.profileList.getSelectedProfile();
    }

    private void copyProfile() {
        Profile selectedProfile = this.profileList.getSelectedProfile();
        Profile copiedProfile = selectedProfile != null
                ? selectedProfile.copy()
                : new Profile(NO_PROFILE_TEXT.getString() + " - " + COPY_TEXT.getString());

        this.copyListener.onCopy(selectedProfile, copiedProfile);
        this.config.addProfile(copiedProfile);
        this.setProfile(copiedProfile);
        this.sidebar.setOpen(false);
        this.profileList.refresh();
    }

    public interface CopyListener {
        void onCopy(@Nullable Profile original, @NotNull Profile copy);
    }
}
