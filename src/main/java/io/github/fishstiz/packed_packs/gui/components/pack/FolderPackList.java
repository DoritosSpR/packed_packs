package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.packed_packs.gui.components.events.*;
import io.github.fishstiz.packed_packs.pack.PackAssets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FolderPackList extends CurrentPackList {
    public FolderPackList(PackAssets packAssets, PackListEventListener listener) {
        super(packAssets, listener);
    }

    @Override
    protected @NotNull Entry createEntry(Pack pack, int index) {
        return new SubPackEntry(pack, index);
    }

    @Override
    public boolean isTransferable(Pack pack) {
        return false;
    }

    @Override
    public boolean canDrop(PackListBase source, List<Pack> payload, Pack trigger, double mouseX, double mouseY) {
        return source == this && super.canDrop(source, payload, trigger, mouseX, mouseY);
    }

    @Override
    public void renderDroppableZone(GuiGraphics guiGraphics, PackListBase source, List<Pack> payload, Pack trigger, int mouseX, int mouseY, float partialTick) {
        if (source == this) {
            super.renderDroppableZone(guiGraphics, source, payload, trigger, mouseX, mouseY, partialTick);
        }
    }

    protected class SubPackEntry extends Entry {
        protected SubPackEntry(Pack pack, int index) {
            super(pack, index);
        }

        @Override
        public boolean isTransferable() {
            return false;
        }
    }
}
