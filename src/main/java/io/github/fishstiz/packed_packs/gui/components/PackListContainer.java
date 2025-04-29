package io.github.fishstiz.packed_packs.gui.components;

import io.github.fishstiz.packed_packs.gui.event.*;
import io.github.fishstiz.packed_packs.gui.components.list.PackList;
import io.github.fishstiz.packed_packs.util.pack.PackIconCache;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public abstract class PackListContainer extends Screen implements DragEventListener, PackIconCache {
    private DragEvent dragged;

    protected PackListContainer(Component title) {
        super(title);
    }

    protected void focus(ComponentPath path) {
        this.clearFocus();
        path.applyFocus(true);
    }

    protected void focus(GuiEventListener element) {
        this.focus(ComponentPath.path(element, this));
    }

    protected void focusList(PackList packList) {
        PackList.Entry entry = packList.getSelected();

        if (entry != null) {
            this.focus(ComponentPath.path(entry, packList, this));
        } else {
            this.focus(ComponentPath.path(packList, this));
        }
    }

    @Override
    public @Nullable DragEvent getDragged() {
        return this.dragged;
    }

    @Override
    public void setDragged(@Nullable DragEvent dragged) {
        this.dragged = dragged;
    }

    public abstract void onEvent(Event event);

    @NotNull
    @Unmodifiable
    protected abstract List<PackList> getLists();

    public abstract PackList getTarget(PackList source);

    protected PackList.Snapshot[] takeSnapshots() {
        List<PackList> lists = this.getLists();
        PackList.Snapshot[] snapshots = new PackList.Snapshot[lists.size()];

        for (int i = 0; i < lists.size(); i++) {
            snapshots[i] = lists.get(i).captureState();
        }

        return snapshots;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.renderDragged(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.isDraggingSelection() || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.isDraggingSelection() || super.charTyped(codePoint, modifiers);
    }
}
