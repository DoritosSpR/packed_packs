package io.github.fishstiz.packed_packs.gui.components.profile;

import io.github.fishstiz.fidgetz.gui.components.AbstractFixedListWidget;
import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuProvider;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.ARGBColor;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.fidgetz.util.debounce.PollingDebouncer;
import io.github.fishstiz.fidgetz.util.debounce.SimplePollingDebouncer;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.*;

public class ProfileList extends AbstractFixedListWidget<ProfileList.Entry> implements ContextMenuContainer {
    private static final int ITEM_HEIGHT = 20;
    private static final Component EMPTY_TEXT = ResourceUtil.getText("profile.empty");
    private static final Component DELETE_TEXT = ResourceUtil.getText("profile.delete");
    private static final Tooltip DELETE_INFO = Tooltip.create(ResourceUtil.getText("profile.delete.info"));
    private static final Sprite TRASH_SPRITE = Sprite.of16(ResourceUtil.getIcon("trash"));
    private static final Sprite STAR_OUTLINE_SPRITE = Sprite.of16(ResourceUtil.getIcon("star_outline"));
    private final PollingDebouncer<Void> debouncedRefresh = new SimplePollingDebouncer<>(this::refresh, 200);
    private final BiConsumer<Profile, Profile> selectListener;
    private final Consumer<Profile> updateListener;
    private final Config.Packs userConfig;
    private final DevConfig.Packs config;
    private @Nullable Profile selectedProfile;

    public ProfileList(
            Config.Packs userConfig,
            DevConfig.Packs Config,
            BiConsumer<Profile, Profile> selectListener,
            Consumer<Profile> updateListener
    ) {
        super(ITEM_HEIGHT, DEFAULT_SCROLLBAR_OFFSET, 0, 0);
        this.userConfig = userConfig;
        this.config = Config;
        this.selectListener = selectListener;
        this.updateListener = updateListener;
    }

    public void scheduleRefresh() {
        this.debouncedRefresh.run();
    }

    public void refresh() {
        this.clearEntries();

        int i = 0;

        Profile defaultProfile = this.config.getDefaultProfile();
        if (defaultProfile != null) {
            this.addEntry(new Entry(defaultProfile, i++));
        }

        List<Profile> profiles = this.userConfig.getProfiles();
        for (Profile profile : profiles) {
            if (defaultProfile != null && Objects.equals(profile.getId(), defaultProfile.getId())) continue;
            this.addEntry(new Entry(profile, i++));
        }
    }

    public @Nullable Profile getSelectedProfile() {
        return this.selectedProfile;
    }

    public void selectProfile(@Nullable Profile profile) {
        Profile previous = this.selectedProfile;
        this.selectedProfile = profile;
        this.selectListener.accept(previous, profile);
        this.updateListener.accept(profile);
    }

    public void removeProfile(Profile profile) {
        if (profile != null && this.selectedProfile == profile) {
            List<Profile> profiles = this.userConfig.getProfiles();
            if (!profiles.isEmpty()) {
                int index = profiles.indexOf(profile);
                Profile previous = (index > 0) ? profiles.get(index - 1) : null;
                this.selectProfile(previous);
            } else {
                this.selectProfile(null);
            }
        }
        if (profile != null) {
            this.userConfig.removeProfile(profile);
        }
        this.refresh();
    }

    private void resync() {
        this.selectListener.accept(this.selectedProfile, this.selectedProfile);
        this.updateListener.accept(this.selectedProfile);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.debouncedRefresh.poll();

        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        if (this.children().isEmpty()) {
            renderScrollingString(
                    guiGraphics,
                    this.minecraft.font,
                    EMPTY_TEXT,
                    this.getX() + SPACING,
                    this.getY() + SPACING,
                    this.getRight() - SPACING,
                    this.getBottom() - SPACING,
                    Theme.WHITE.getARGB()
            );
        }
    }

    public class Entry extends AbstractFixedListWidget<Entry>.Entry implements ContextMenuProvider {
        private final Profile profile;
        private final List<FidgetzButton<Void>> children = new ArrayList<>();
        private final FidgetzButton<Void> selectButton;
        private final FidgetzButton<Void> deleteButton;

        protected Entry(Profile profile, int index) {
            super(index);

            this.profile = profile;
            this.deleteButton = FidgetzButton.<Void>builder()
                    .makeSquare(this.getHeight())
                    .setMessage(DELETE_TEXT)
                    .setSprite(this.isDefault()
                            ? ButtonSprites.of(STAR_SPRITE) : profile.isLocked()
                            ? ButtonSprites.unclamp(LOCK_SPRITE) : ButtonSprites.of(TRASH_SPRITE))
                    .setOnPress(this::remove)
                    .build();
            this.deleteButton.active = !profile.isLocked() && !this.isDefault();
            if (this.deleteButton.active) this.deleteButton.setTooltip(DELETE_INFO);

            this.selectButton = FidgetzButton.<Void>builder()
                    .setMessage(Component.literal(this.profile.getName()))
                    .setOnPress(this::select)
                    .build();

            this.children.add(this.deleteButton);
            this.children.add(this.selectButton);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            this.selectButton.active = ProfileList.this.selectedProfile != this.profile;

            this.deleteButton.setPosition(left, top);
            this.selectButton.setPosition(left + this.deleteButton.getWidth(), top);
            this.selectButton.setWidth(width - this.deleteButton.getWidth());

            this.deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.selectButton.render(guiGraphics, mouseX, mouseY, partialTick);

            if (Config.get().isDevMode()) {
                boolean hasProperty = true;
                int borderColor;

                if (this.isDefault() && this.profile.isLocked()) {
                    borderColor = Theme.PURPLE_500.getARGB();
                } else if (this.isDefault()) {
                    borderColor = Theme.BLUE_500.getARGB();
                } else if (this.profile.isLocked()) {
                    borderColor = Theme.RED_700.getARGB();
                } else {
                    borderColor = Theme.WHITE.getARGB();
                    hasProperty = false;
                }

                boolean hovered = guiGraphics.containsPointInScissor(mouseX, mouseY) && GuiUtil.isHovered(this, mouseX, mouseY);
                if (hasProperty || hovered) {
                    guiGraphics.renderOutline(left, top, width, height, borderColor);
                }
                if (hovered) {
                    int foregroundColor = ARGBColor.withAlpha(borderColor, 0.25f);
                    guiGraphics.fill(left, top, left + width, top + height, foregroundColor);
                }
            }
        }

        private void toggleLock() {
            boolean selected = this.isSelected();
            if (selected) ProfileList.this.resync();

            this.profile.setLocked(!this.profile.isLocked());
            ProfileList.this.refresh();
            if (selected) ProfileList.this.updateListener.accept(this.profile);
        }

        private void toggleDefault() {
            ProfileList.this.config.setDefaultProfile(this.isDefault() ? null : this.profile);
            ProfileList.this.refresh();

            if (!this.isSelected() && this.isDefault()) {
                this.select();
            } else {
                ProfileList.this.resync();
            }
        }

        private void remove() {
            ProfileList.this.removeProfile(this.profile);
        }

        private void select() {
            ProfileList.this.selectProfile(this.profile);
        }

        private boolean isDefault() {
            return Objects.equals(this.profile, ProfileList.this.config.getDefaultProfile());
        }

        private boolean isSelected() {
            return Objects.equals(this.profile, ProfileList.this.selectedProfile);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> consumer) {
            this.children.forEach(consumer);
        }

        @Override
        public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
            if (!Config.get().isDevMode()) return;

            builder.separatorIfNonEmpty();
            builder.add(GuiConstants.devItem(ResourceUtil.getText("profile.default." + (this.isDefault() ? "unset" : "set")))
                    .icon(() -> this.isDefault() ? STAR_SPRITE : STAR_OUTLINE_SPRITE)
                    .action(this::toggleDefault)
                    .build());
            builder.add(GuiConstants.devItem(ResourceUtil.getText("profile." + (this.profile.isLocked() ? "unlock" : "lock")))
                    .icon(this.profile.isLocked() ? LOCK_SPRITE_SMALL : UNLOCK_SPRITE_SMALL)
                    .action(this::toggleLock)
                    .build());
        }
    }
}
