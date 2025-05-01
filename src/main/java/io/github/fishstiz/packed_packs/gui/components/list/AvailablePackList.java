package io.github.fishstiz.packed_packs.gui.components.list;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.fidgetz.gui.Background;
import io.github.fishstiz.fidgetz.gui.sprites.Sprite;
import io.github.fishstiz.packed_packs.gui.event.PackListEventListener;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.util.pack.PackIconCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static io.github.fishstiz.fidgetz.util.WidgetUtil.isPointWithinBounds;
import static io.github.fishstiz.fidgetz.util.WidgetUtil.playClickSound;
import static io.github.fishstiz.packed_packs.util.InputUtil.isLeftClick;
import static io.github.fishstiz.packed_packs.util.ResourceUtil.getVanillaSprite;
import static io.github.fishstiz.packed_packs.util.lang.ObjectsUtil.pick;

public final class AvailablePackList extends PackListBase<AvailablePackList.Entry> {
    private static final Sprite SELECT_HIGHLIGHTED_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/select_highlighted"));
    private static final Sprite SELECT_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/select"));
    private static final Theme DROP_ZONE_THEME = Theme.RED_700;
    private static final Background.Color DROP_ZONE = new Background.Color(DROP_ZONE_THEME.withAlpha(0.25f));

    public AvailablePackList(PackIconCache iconCache, PackListEventListener listener) {
        super(iconCache, listener);
    }

    @Override
    protected @NotNull Entry createEntry(Pack pack, int index) {
        return new Entry(pack, index);
    }

    private boolean isInvalidDrop(PackList source, List<Pack> selection) {
        return source == this || selection.isEmpty() || !source.isTransferable(selection.getLast());
    }

    @Override
    protected @Nullable List<Pack> handleDrop(PackList source, ImmutableList<Pack> selection, double mouseX, double mouseY) {
        if (this.isInvalidDrop(source, selection)) return null;

        this.clearSelection();
        source.clearSelection();

        List<Pack> dropped = new ArrayList<>();
        for (Pack selected : selection) {
            if (source.isTransferable(selected)) {
                source.remove(selected);
                dropped.add(selected);
                this.add(selected);
                this.select(selected);
            }
        }
        return dropped;
    }

    @Override
    public void renderDroppableZone(GuiGraphics guiGraphics, PackList source, List<Pack> selection, int mouseX, int mouseY, float partialTick) {
        if (this.isInvalidDrop(source, selection)) return;

        int width = this.scrollbarVisible() ? this.getWidth() - this.scrollbarOffset : this.getWidth();

        if (this.isMouseOver(mouseX, mouseY)) {
            DROP_ZONE.render(guiGraphics, this.getX(), this.getY(), width, this.getHeight());
        }

        guiGraphics.renderOutline(this.getX(), this.getY(), width, this.getHeight(), DROP_ZONE_THEME.getARGB());
    }

    public class Entry extends PackListBase<Entry>.Entry {
        private Entry(Pack pack, int index) {
            super(pack, index);
        }

        @Override
        public boolean isTransferable() {
            return true;
        }

        public boolean isSelectMouseOver(double mouseX, double mouseY) {
            return isPointWithinBounds(this.getX() + SPACING, this.getY(), SELECT_SPRITE.width, SELECT_SPRITE.height, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isLeftClick(button) && this.isSelectMouseOver(mouseX, mouseY)) {
                playClickSound();
                this.transfer();
                return false;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected void renderForeground(GuiGraphics guiGraphics, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            if (!hovering && !this.isSelectedLast()) return;

            int x = left + SPACING;
            OVERLAY.render(guiGraphics, x, top, SELECT_SPRITE.width, SELECT_SPRITE.height);
            pick(!this.isSelectMouseOver(mouseX, mouseY), SELECT_SPRITE, SELECT_HIGHLIGHTED_SPRITE).render(guiGraphics, x, top);
        }
    }
}
