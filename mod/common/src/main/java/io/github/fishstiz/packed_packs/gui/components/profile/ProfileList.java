package io.github.fishstiz.packed_packs.gui.components.profile;

import io.github.fishstiz.fidgetz.gui.components.AbstractFixedListWidget;
import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuContainer;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuProvider;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.ButtonSprites;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.ARGBColor;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.fidgetz.util.debounce.PollingDebouncer;
import io.github.fishstiz.fidgetz.util.debounce.SimplePollingDebouncer;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.components.events.ActionDispatcher;
import io.github.fishstiz.packed_packs.gui.components.events.ProfileEvent;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
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
    private final PackOptionsContext options;
    private final ActionDispatcher eventHandler;

    public ProfileList(PackOptionsContext options, ActionDispatcher eventHandler) {
        super(ITEM_HEIGHT);
        this.options = options;
        this.eventHandler = eventHandler;
    }

    public void scheduleRefresh() {
        this.debouncedRefresh.run();
    }

    public void refresh() {
        this.clearEntries();

        int i = 0;

        Profile defaultProfile = this.options.getConfig().getDefaultProfile();
        if (defaultProfile != null) {
            this.addEntry(new Entry(defaultProfile, i++));
        }

        List<Profile> profiles = this.options.getUserConfig().getProfiles();
        for (Profile profile : profiles) {
            if (defaultProfile != null && Objects.equals(profile.getId(), defaultProfile.getId())) continue;
            this.addEntry(new Entry(profile, i++));
        }
    }

    @Override
    public void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.debouncedRefresh.poll();

        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        if (this.children().isEmpty()) {
            this.renderScrollingStringOverContents(guiGraphics.textRenderer(), EMPTY_TEXT, 0);
        }
    }

    public class Entry extends AbstractFixedListWidget<Entry>.Entry implements ContextMenuProvider {
        private final Profile profile;
        private final List<FidgetzButton<Void>> children;
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
            this.selectButton.active = !this.isSelected();

            this.children = List.of(this.deleteButton, this.selectButton);
        }

        @Override
        public void renderContent(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int left = this.getX();
            int top = this.getY();

            this.deleteButton.setPosition(left, top);
            this.selectButton.setPosition(left + this.deleteButton.getWidth(), top);
            this.selectButton.setWidth(width - this.deleteButton.getWidth());

            this.deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.selectButton.render(guiGraphics, mouseX, mouseY, partialTick);

            if (Config.get().isDevMode()) {
                boolean hasProperty = true;
                int width = this.getWidth();
                int height = this.getHeight();
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
                    DrawUtil.renderOutline(guiGraphics, left, top, width, height, borderColor);
                }
                if (hovered) {
                    int foregroundColor = ARGBColor.withAlpha(borderColor, 0.25f);
                    guiGraphics.fill(left, top, left + width, top + height, foregroundColor);
                }
            }
        }

        private void sendEvent(ProfileEvent event) {
            ProfileList.this.eventHandler.dispatch(event);
        }

        private void select() {
            this.sendEvent(new ProfileEvent.Select(this.profile));
        }

        private void toggleLock() {
            this.sendEvent(new ProfileEvent.ToggleLock(this.profile));
        }

        private void toggleDefault() {
            this.sendEvent(new ProfileEvent.ToggleDefault(this.profile));
        }

        private void remove() {
            this.sendEvent(new ProfileEvent.Delete(this.profile));
        }

        private boolean isDefault() {
            return Objects.equals(this.profile, ProfileList.this.options.getConfig().getDefaultProfile());
        }

        private boolean isSelected() {
            return ProfileList.this.options.getProfile().map(profile -> profile.equals(this.profile)).orElse(false);
        }

        @Override
        public @NonNull List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public @NonNull List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        @Override
        public void visitWidgets(@NonNull Consumer<AbstractWidget> consumer) {
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
