package io.github.fishstiz.packed_packs.gui.components.pack;

import com.google.common.collect.ImmutableList;
import io.github.fishstiz.packed_packs.config.Folder;
import io.github.fishstiz.packed_packs.gui.components.events.*;
import io.github.fishstiz.packed_packs.pack.PackAssets;
import io.github.fishstiz.packed_packs.pack.folder.FolderPack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FolderPackList extends CurrentPackList {
    private @Nullable FolderPack folderPack;

    public FolderPackList(PackAssets packAssets, PackListEventListener listener) {
        super(packAssets, listener);
    }

    @Override
    protected @NotNull Entry createEntry(Pack pack, int index) {
        return new SubPackEntry(pack, index);
    }

    public void onFolderPackChange(@Nullable FolderPack folderPack) {
        this.folderPack = folderPack;
    }

    @Override
    public boolean isTransferable(Pack pack) {
        return false;
    }

    @Override
    public boolean canDrop(PackList source, ImmutableList<Pack> payload, Pack trigger, double mouseX, double mouseY) {
        return source == this && super.canDrop(source, payload, trigger, mouseX, mouseY);
    }

    @Override
    public boolean isLocked() {
        Folder folder = this.packAssets.getFolderConfig(this.folderPack);
        return (folder != null && folder.isLocked()) || super.isLocked();
    }

    @Override
    public void renderDroppableZone(GuiGraphics guiGraphics, PackList source, ImmutableList<Pack> payload, Pack trigger, int mouseX, int mouseY, float partialTick) {
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
