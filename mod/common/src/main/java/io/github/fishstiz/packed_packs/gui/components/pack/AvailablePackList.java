package io.github.fishstiz.packed_packs.gui.components.pack;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.gui.components.actions.PackListAction;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static io.github.fishstiz.fidgetz.util.GuiUtil.playClickSound;
import static io.github.fishstiz.packed_packs.util.InputUtil.isLeftClick;
import static io.github.fishstiz.packed_packs.util.ResourceUtil.getVanillaSprite;
import static io.github.fishstiz.fidgetz.util.lang.ObjectsUtil.pick;

public class AvailablePackList extends PackList {
    private static final Sprite SELECT_HIGHLIGHTED_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/select_highlighted"));
    private static final Sprite SELECT_SPRITE = Sprite.of32(getVanillaSprite("transferable_list/select"));
    private static final Theme DROP_ZONE_THEME = Theme.RED_700;
    private static final ColoredRect DROP_ZONE = new ColoredRect(DROP_ZONE_THEME.withAlpha(0.25f));

    public AvailablePackList(PackListProps props) {
        super(props);
    }

    @Override
    protected @NonNull Entry createEntry(PackContext context, int index) {
        return new Entry(context, index);
    }

    @Override
    public boolean canInteract(PackList source) {
        return source != this;
    }

    private boolean isInvalidDrop(PackList source, List<Pack> payload, Pack trigger) {
        return !source.canInteract(this) || payload.isEmpty() || !source.isTransferable(trigger);
    }

    @Override
    public boolean canDrop(PackListAction.Drag dragged, double mouseX, double mouseY) {
        return this.isMouseOver(mouseX, mouseY) && !this.isInvalidDrop(dragged.source(), dragged.payload(), dragged.pack());
    }

    @Override
    protected void handleDrop(PackListAction.Drag dragged, double mouseX, double mouseY) {
        PackList source = dragged.source();
        List<Pack> payload = dragged.payload();
        Pack trigger = dragged.pack();

        if (!this.isInvalidDrop(source, payload, trigger)) {
            this.dispatch(new PackListAction.Transfer(source, this, trigger, payload, 0));
        }
    }

    @Override
    public void renderDroppableZone(GuiGraphics guiGraphics, PackListAction.Drag dragged, int mouseX, int mouseY, float partialTick) {
        PackList source = dragged.source();
        List<Pack> payload = dragged.payload();
        Pack trigger = dragged.pack();

        if (this.isInvalidDrop(source, payload, trigger)) return;

        int width = this.scrollbarVisible() ? this.getWidth() - this.scrollbarOffset : this.getWidth();

        if (this.isMouseOver(mouseX, mouseY)) {
            DROP_ZONE.render(guiGraphics, this.getX(), this.getY(), width, this.getHeight(), partialTick);
        }

        DrawUtil.renderOutline(guiGraphics, this.getX(), this.getY(), width, this.getHeight(), DROP_ZONE_THEME.getARGB());
    }

    public class Entry extends PackList.Entry {
        private Entry(PackContext packContext, int index) {
            super(packContext, index);
        }

        public boolean isMouseOverSelect(double mouseX, double mouseY) {
            return AvailablePackList.this.isHovered() && GuiUtil.containsPoint(this.getX() + H_SPACING, this.getY(), SELECT_SPRITE.width, SELECT_SPRITE.height, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(@NonNull MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
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
        protected void handleDevMenuEvent(PackListDevMenu.Event<?> event) {
            super.handleDevMenuEvent(event);
            if (event instanceof PackListDevMenu.Event.Require(Pack trigger, Boolean value, List<Pack> required) &&
                Boolean.TRUE.equals(value)) {
                this.sendPacks(trigger, required);
            }
        }
    }
}
