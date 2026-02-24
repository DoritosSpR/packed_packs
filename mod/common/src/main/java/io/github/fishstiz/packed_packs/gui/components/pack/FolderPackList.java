package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.gui.components.events.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;

public class FolderPackList extends CurrentPackList {
    public FolderPackList(PackListProps props) {
        super(props);
    }

    @Override
    protected @NonNull Entry createEntry(PackContext context, int index) {
        return new SubPackEntry(context, index);
    }

    @Override
    public boolean isTransferable(Pack pack) {
        return false;
    }

    @Override
    public boolean canInteract(PackList source) {
        return source == this;
    }

    @Override
    public boolean canDrop(PackListAction.Drag dragged, double mouseX, double mouseY) {
        return this.canInteract(dragged.source()) && super.canDrop(dragged, mouseX, mouseY);
    }

    @Override
    public void renderDroppableZone(GuiGraphics guiGraphics, PackListAction.Drag dragged, int mouseX, int mouseY, float partialTick) {
        if (dragged.source() == this) {
            super.renderDroppableZone(guiGraphics, dragged, mouseX, mouseY, partialTick);
        }
    }

    protected class SubPackEntry extends Entry {
        protected SubPackEntry(PackContext context, int index) {
            super(context, index);
        }

        @Override
        public boolean isTransferable() {
            return false;
        }
    }
}
