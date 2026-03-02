package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleableDialogContainer;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.components.profile.ProfileList;
import io.github.fishstiz.packed_packs.gui.components.profile.Sidebar;
import io.github.fishstiz.packed_packs.gui.model.ProfilesViewModel;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;

import java.util.Objects;

import static io.github.fishstiz.packed_packs.gui.model.ProfilesViewModel.*;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;

public class ProfilesLayout {
    private static final int MAX_WIDTH = SPACING * 20;
    private final ProfilesViewModel viewModel;
    private final ButtonSprites toggleSprites;
    private final Sidebar sidebar;
    private final ProfileList profileList;
    private ToggleableEditBox<Void> nameField;
    private FidgetzButton<Void> toggleNameButton;
    private FidgetzButton<Void> noProfileButton;
    private boolean initialized = false;

    public <S extends Screen & ToggleableDialogContainer> ProfilesLayout(S screen, ProfilesViewModel viewModel) {
        this.viewModel = viewModel;
        this.toggleSprites = new ButtonSprites(
                Sprite.of16(ResourceUtil.getIcon("edit")),
                Sprite.of16(ResourceUtil.getIcon("edit_inactive"))
        );
        this.sidebar = new Sidebar(screen);
        this.profileList = new ProfileList(viewModel);
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
                .setOnPress(this.viewModel::unselect)
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

        this.refresh();
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
        this.viewModel.renameSelected(value);
        this.profileList.scheduleRefresh();
    }

    public void refresh() {
        if (!this.initialized) return;

        Profile selectedProfile = this.viewModel.selectedProfile();
        boolean hasProfile = selectedProfile != null;
        this.nameField.setEditable(false);
        this.nameField.setHint(hasProfile ? UNNAMED_TEXT : NO_PROFILE_TEXT);
        this.nameField.setValueSilently(hasProfile ? selectedProfile.getName() : "");
        this.nameField.visible = hasProfile;
        this.nameField.active = hasProfile;
        this.noProfileButton.active = hasProfile;
        this.toggleNameButton.visible = hasProfile;
        this.toggleNameButton.active = hasProfile && !selectedProfile.isLocked();

        if (hasProfile && selectedProfile.isLocked()) {
            ButtonSprites sprites = Objects.equals(selectedProfile, this.viewModel.defaultProfile())
                    ? ButtonSprites.of(STAR_SPRITE)
                    : ButtonSprites.unclamp(LOCK_SPRITE);
            this.toggleNameButton.setSprites(sprites);
        } else {
            this.toggleNameButton.setSprites(this.toggleSprites);
        }

        this.profileList.refresh();
    }

    private void copyProfile() {
        this.viewModel.copySelected();
        this.sidebar.setOpen(false);
        this.profileList.refresh();
    }
}
