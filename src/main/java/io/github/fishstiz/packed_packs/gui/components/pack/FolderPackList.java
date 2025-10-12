package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.packed_packs.gui.components.SelectionContext;
import io.github.fishstiz.packed_packs.gui.components.events.*;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.pack.PackFileOperations;
import io.github.fishstiz.packed_packs.pack.PackOptionsContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FolderPackList extends CurrentPackList {
    public FolderPackList(PackOptionsContext options, PackAssetManager assets, PackFileOperations fileOps, PackListEventListener listener) {
        super(options, assets, fileOps, listener);
    }

    @Override
    protected @NotNull Entry createEntry(SelectionContext<Pack> context, int index) {
        return new SubPackEntry(context, index);
    }

    @Override
    public boolean isTransferable(Pack pack) {
        return false;
    }

    @Override
    public boolean canDrop(PackList source, List<Pack> payload, Pack trigger, double mouseX, double mouseY) {
        return source == this && super.canDrop(source, payload, trigger, mouseX, mouseY);
    }

    @Override
    public void renderDroppableZone(GuiGraphics guiGraphics, PackList source, List<Pack> payload, Pack trigger, int mouseX, int mouseY, float partialTick) {
        if (source == this) {
            super.renderDroppableZone(guiGraphics, source, payload, trigger, mouseX, mouseY, partialTick);
        }
    }

    protected class SubPackEntry extends Entry {
        protected SubPackEntry(SelectionContext<Pack> context, int index) {
            super(context, index);
        }

        @Override
        public boolean isTransferable() {
            return false;
        }
    }
}
