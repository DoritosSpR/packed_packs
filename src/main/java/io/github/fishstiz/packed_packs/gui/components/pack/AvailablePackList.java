package io.github.fishstiz.packed_packs.gui.components.pack;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.components.events.PackListEventListener;
import io.github.fishstiz.packed_packs.gui.metadata.Toggleable;
import io.github.fishstiz.packed_packs.util.ResourceUtil;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.pack.PackAssets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static io.github.fishstiz.fidgetz.util.GuiUtil.playClickSound;
import static io.github.fishstiz.packed_packs.util.InputUtil.isLeftClick;
import static io.github.fishstiz.packed_packs.util.ResourceUtil.getVanillaSprite;
import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.devItem;
import static io.github.fishstiz.packed_packs.util.lang.ObjectsUtil.pick;
import static java.util.Optional.ofNullable;

public class AvailablePackList extends PackListBase<AvailablePackList.Entry> {
    private static final Sprite SELECT_HIGHLIGHTED_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/select_highlighted"));
    private static final Sprite SELECT_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/select"));
    private static final Theme DROP_ZONE_THEME = Theme.RED_700;
    private static final ColoredRect DROP_ZONE = new ColoredRect(DROP_ZONE_THEME.withAlpha(0.25f));

    public AvailablePackList(PackAssets packAssets, PackListEventListener listener) {
        super(packAssets, listener);
    }

    @Override
    protected @NotNull Entry createEntry(Pack pack, int index) {
        return new Entry(pack, index);
    }

    private boolean isInvalidDrop(PackList source, ImmutableList<Pack> payload, Pack trigger) {
        return source == this || source instanceof FolderPackList || payload.isEmpty() || !source.isTransferable(trigger);
    }

    @Override
    public boolean canDrop(PackList source, ImmutableList<Pack> payload, Pack trigger, double mouseX, double mouseY) {
        return this.isMouseOver(mouseX, mouseY) && !this.isInvalidDrop(source, payload, trigger);
    }

    @Override
    protected @Nullable List<Pack> handleDrop(PackList source, ImmutableList<Pack> payload, Pack trigger, double mouseX, double mouseY) {
        if (this.isInvalidDrop(source, payload, trigger)) return null;

        List<Pack> dropped = new ArrayList<>();
        for (Pack pack : payload) {
            if (source.isTransferable(pack)) {
                dropped.add(pack);
            }
        }

        this.clearSelection();
        source.removeAll(dropped);
        this.addAll(dropped);
        this.selectAll(dropped);
        this.select(trigger);
        ofNullable(this.getEntry(trigger)).ifPresent(this::scrollToEntry);

        return dropped;
    }

    @Override
    public void renderDroppableZone(GuiGraphics guiGraphics, PackList source, ImmutableList<Pack> payload, Pack trigger, int mouseX, int mouseY, float partialTick) {
        if (this.isInvalidDrop(source, payload, trigger)) return;

        int width = this.scrollbarVisible() ? this.getWidth() - this.scrollbarOffset : this.getWidth();

        if (this.isMouseOver(mouseX, mouseY)) {
            DROP_ZONE.render(guiGraphics, this.getX(), this.getY(), width, this.getHeight(), partialTick);
        }

        DrawUtil.renderOutline(guiGraphics, this.getX(), this.getY(), width, this.getHeight(), DROP_ZONE_THEME.getARGB());
    }

    public class Entry extends PackListBase<Entry>.Entry {
        private Entry(Pack pack, int index) {
            super(pack, index);
        }

        @Override
        public boolean isTransferable() {
            return !this.isStale() && !AvailablePackList.this.isLocked();
        }

        public boolean isMouseOverSelect(double mouseX, double mouseY) {
            return AvailablePackList.this.isHovered() && GuiUtil.containsPoint(this.getX() + H_SPACING, this.getY(), SELECT_SPRITE.width, SELECT_SPRITE.height, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
            if (isLeftClick(mouseButtonEvent) && this.isMouseOverSelect(mouseButtonEvent.x(), mouseButtonEvent.y())) {
                playClickSound();
                this.transfer();
                return false;
            }

            return super.mouseClicked(mouseButtonEvent, doubleClicked);
        }

        @Override
        protected void renderForeground(GuiGraphics guiGraphics, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            if (!hovering && !this.isSelectedLast()) return;

            int x = left + H_SPACING;
            GuiConstants.WHITE_OVERLAY.render(guiGraphics, x, top, SELECT_SPRITE.width, SELECT_SPRITE.height);
            if (this.isTransferable()) {
                boolean overSelect = this.isMouseOverSelect(mouseX, mouseY);
                pick(!overSelect, SELECT_SPRITE, SELECT_HIGHLIGHTED_SPRITE).render(guiGraphics, x, top);
                if (overSelect) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        }

        @Override
        protected void onBuildHeader(ContextMenuItemBuilder builder) {
            Profile profile = PackedPacks.CONFIG.isDevMode() ? AvailablePackList.this.packAssets.getProfile() : null;
            if (profile == null) return;

            PackOverride override = this.hasOverride(Profile::isHidden);
            builder.add(devItem(HIDDEN)
                    .icon(() -> override == PackOverride.GLOBAL
                            ? Sprite.of16(ResourceUtil.getIcon("radio_globe"))
                            : Toggleable.getDefaultIcon(profile.isHidden(this.pack))
                    )
                    .activeWhen(() -> !AvailablePackList.this.isLocked() && override != PackOverride.GLOBAL)
                    .action(() -> this.updateHidden(!profile.isHidden(this.pack)))
                    .closeOnInteract(false)
                    .build());
            builder.separator();
        }
    }
}
