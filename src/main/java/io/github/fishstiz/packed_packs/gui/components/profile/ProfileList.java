package io.github.fishstiz.packed_packs.gui.components.profile;

import io.github.fishstiz.fidgetz.gui.components.AbstractFixedListWidget;
import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.Size;
import io.github.fishstiz.fidgetz.util.debounce.PollingDebouncer;
import io.github.fishstiz.fidgetz.util.debounce.SimplePollingDebouncer;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProfileList extends AbstractFixedListWidget<ProfileList.Entry> {
    private static final int ITEM_HEIGHT = 20;
    private static final Component EMPTY_TEXT = ResourceUtil.getText("profile.empty");
    private static final Component DELETE_TEXT = ResourceUtil.getText("profile.delete");
    private static final Tooltip DELETE_INFO = Tooltip.create(ResourceUtil.getText("profile.delete.info"));
    private static final Sprite TRASH_SPRITE = new Sprite(ResourceUtil.getIcon("trash"), Size.of16());
    private final PollingDebouncer<Void> debouncedRefresh = new SimplePollingDebouncer<>(this::refresh, 200);
    private final Config.Packs config;
    private final Supplier<Profile> selected;
    private final Consumer<Profile> onDelete;
    private final Consumer<Profile> onSelect;
    private List<Profile> profiles;

    public ProfileList(Config.Packs config, Supplier<Profile> selected, Consumer<Profile> onDelete, Consumer<Profile> onSelect) {
        super(ITEM_HEIGHT);

        this.config = config;
        this.selected = selected;
        this.onDelete = onDelete;
        this.onSelect = onSelect;

        this.refresh();
    }

    public void scheduleRefresh() {
        this.debouncedRefresh.run();
    }

    public void refresh() {
        this.clearEntries();

        this.profiles = this.config.getProfiles();
        for (int i = 0; i < this.profiles.size(); i++) {
            this.addEntry(new Entry(this.profiles.get(i), i));
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.debouncedRefresh.poll();

        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        if (this.profiles == null || this.profiles.isEmpty()) {
            int padding = 8;

            renderScrollingString(
                    guiGraphics,
                    this.minecraft.font,
                    EMPTY_TEXT,
                    this.getX() + padding,
                    this.getY() + padding,
                    this.getRight() - padding,
                    this.getBottom() - padding,
                    Theme.WHITE.getARGB()
            );
        }
    }

    public class Entry extends AbstractFixedListWidget<Entry>.Entry {
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
                    .setTooltip(DELETE_INFO)
                    .setSprite(TRASH_SPRITE)
                    .setOnPress(() -> ProfileList.this.onDelete.accept(this.profile))
                    .build();
            this.selectButton = FidgetzButton.<Void>builder()
                    .setMessage(Component.literal(this.profile.getName()))
                    .setOnPress(() -> ProfileList.this.onSelect.accept(this.profile))
                    .build();

            this.children.add(this.deleteButton);
            this.children.add(this.selectButton);
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            this.selectButton.active = ProfileList.this.selected.get() != this.profile;

            int left = this.getX();
            int top = this.getY();

            this.deleteButton.setPosition(left, top);
            this.selectButton.setPosition(left + this.deleteButton.getWidth(), top);
            this.selectButton.setWidth(width - this.deleteButton.getWidth());

            this.deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.selectButton.render(guiGraphics, mouseX, mouseY, partialTick);
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
    }
}
