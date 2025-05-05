package io.github.fishstiz.packed_packs.gui.layouts;

import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.ToggleableEditBox;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.components.Sidebar;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProfilesLayout {
    private static final Component TITLE_TEXT = ResourceUtil.getText("profile");
    private static final Component NO_PROFILE_TEXT = ResourceUtil.getText("profile.none");
    private static final Component UNNAMED_TEXT = ResourceUtil.getText("profile.unnamed");
    private static final Component NEW_TEXT = ResourceUtil.getText("profile.new");
    private static final Component COPY_TEXT = ResourceUtil.getText("profile.copy");
    private final Config.Packs config;
    private final Sidebar sidebar;
    private final Supplier<List<Pack>> selectedPacks;
    private final Consumer<Profile> listener;
    private final ToggleableEditBox<Void> nameField = ToggleableEditBox.<Void>builder()
            .setHint(UNNAMED_TEXT)
            .setMaxLength(Profile.NAME_MAX_LENGTH)
            .setFilter(value -> value != null && (value.isEmpty() || !value.isBlank()))
            .addListener(this::onNameChange)
            .build();
    private final FidgetzButton<Void> toggleNameButton = FidgetzButton.<Void>builder()
            .setWidth(20)
            .setOnPress(nameField::toggle)
            .build();
    private @Nullable Profile profile;

    public ProfilesLayout(Sidebar.Builder sidebar, Config.Packs config, Supplier<List<Pack>> selectedPacks, Consumer<Profile> listener) {
        this.config = config;
        this.sidebar = sidebar.setTitle(TITLE_TEXT.copy().withColor(Theme.GRAY_800.getARGB()), false).build();
        this.selectedPacks = selectedPacks;
        this.listener = listener;

        this.setProfile(this.config.getLastViewed());
    }

    public void initContents(int spacing) {
        LayoutSettings layoutSettings = LayoutSettings.defaults().paddingHorizontal(spacing);
        FlexLayout firstRow = FlexLayout.horizontal(this.sidebar.root()::getWidth).spacing(spacing);
        FlexLayout secondRow = FlexLayout.horizontal(this.sidebar.root()::getWidth);
        FlexLayout thirdRow = FlexLayout.horizontal(this.sidebar.root()::getWidth);

        firstRow.addFlexChild(FidgetzButton.<Void>builder().setMessage(NEW_TEXT).setOnPress(this::createProfile).build());
        firstRow.addFlexChild(FidgetzButton.<Void>builder().setMessage(COPY_TEXT).setOnPress(this::copyProfile).build());
        secondRow.addFlexChild(FidgetzButton.builder().setMessage(NO_PROFILE_TEXT).setOnPress(() -> {
            this.setProfile(null);
            this.sidebar.setOpen(false);
        }).build());
        thirdRow.addFlexChild(FidgetzButton.builder().build(), true); // TODO profile list (with delete)

        this.sidebar.root().layout().addChild(firstRow, layoutSettings);
        this.sidebar.root().layout().addChild(secondRow, layoutSettings);
        this.sidebar.root().layout().addFlexChild(thirdRow, true, layoutSettings.copy().paddingBottom(spacing + 1));
        this.sidebar.root().layout().arrangeElements();
        this.sidebar.root().layout().visitWidgets(this.sidebar::addRenderableWidget);
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
    }

    private void setProfile(@Nullable Profile profile) {
        if (this.profile != null) {
            this.profile.setPacks(this.selectedPacks.get());
        }

        this.profile = profile;

        this.nameField.setEditable(false);

        boolean active = profile != null;
        this.nameField.setHint(active ? UNNAMED_TEXT : NO_PROFILE_TEXT);
        this.nameField.setValue(active ? profile.getName() : "");
        this.toggleNameButton.active = active;

        this.listener.accept(this.profile);
    }

    public @Nullable Profile getProfile() {
        return this.profile;
    }

    private void createProfile() {
        Profile newProfile = new Profile(UNNAMED_TEXT.getString());
        this.config.addProfile(newProfile);
        this.setProfile(newProfile);
        this.sidebar.setOpen(false);
    }

    private void copyProfile() {
        Profile copiedProfile = this.profile != null
                ? this.profile.copy()
                : new Profile(NO_PROFILE_TEXT.getString() + " - " + COPY_TEXT.getString());

        copiedProfile.setPacks(this.selectedPacks.get());
        this.config.addProfile(copiedProfile);
        this.setProfile(copiedProfile);
        this.sidebar.setOpen(false);
    }

    private void removeProfile(Profile profile) {
        if (profile != null && this.profile == profile) {
            List<Profile> profiles = this.config.getProfiles();
            if (!profiles.isEmpty()) {
                int index = profiles.indexOf(profile);
                int previous = index > 0 ? index - 1 : -1;
                this.setProfile(index != -1 ? profiles.get(previous) : null);
            } else {
                this.setProfile(null);
            }
        }
        this.config.removeProfile(profile);
    }
}
