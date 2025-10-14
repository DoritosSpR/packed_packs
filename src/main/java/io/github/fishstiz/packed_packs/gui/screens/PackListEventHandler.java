package io.github.fishstiz.packed_packs.gui.screens;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.fishstiz.packed_packs.compat.cursors_extended.CursorsExtended;
import io.github.fishstiz.packed_packs.gui.components.events.*;
import io.github.fishstiz.packed_packs.gui.components.pack.PackList;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class PackListEventHandler extends Screen implements PackListEventListener, DragEventHandler {
    protected final PackAssetManager assetManager;
    private final DragEventRenderer dragEventRenderer;
    private DragEvent dragged;

    protected PackListEventHandler(Minecraft minecraft, Component title) {
        super(title);
        this.minecraft = minecraft;
        this.assetManager = new PackAssetManager(minecraft);
        this.dragEventRenderer = new DragEventRenderer(this.assetManager);
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

    protected void transferFocus(PackList source, PackList destination) {
        source.setFocused(null);
        this.focusList(destination);
    }

    protected void unfocusOtherLists(PackList focused) {
        for (PackList packList : this.getPackLists()) {
            if (packList != focused) {
                packList.setFocused(null);
            }
        }
    }

    protected void handleRequestTransferEvent(RequestTransferEvent event) {
        PackList source = event.target();
        PackList destination = this.getDestination(source);

        if (destination == null || event.payload().isEmpty()) {
            return;
        }

        destination.clearSelection();
        source.removeAll(event.payload());
        destination.addAll(event.payload());
        destination.selectAll(event.payload());
        destination.select(event.trigger());

        this.transferFocus(source, destination);
    }

    @Override
    public void handleDragEvent(DragEvent event) {
        DragEventHandler.super.handleDragEvent(event);
        this.unfocusOtherLists(event.target());
    }

    protected void handleMoveEvent(MoveEvent event) {
        PackList.Entry entry = event.target().getEntry(event.trigger());
        if (entry != null) {
            this.focus(ComponentPath.path(entry, event.target(), this));
        } else {
            this.focus(event.target());
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

    protected abstract @NotNull List<PackList> getPackLists();

    protected abstract @Nullable PackList getDestination(PackList source);

    protected abstract boolean isUnlocked();

    @Override
    public void onEvent(PackListEvent event) {
        switch (event) {
            case SelectionEvent selection -> this.unfocusOtherLists(selection.target());
            case RequestTransferEvent request -> this.handleRequestTransferEvent(request);
            case MoveEvent move -> this.handleMoveEvent(move);
            case DragEvent drag -> this.handleDragEvent(drag);
            case DropEvent drop -> this.transferFocus(drop.target(), drop.destination());
            default -> {
            }
        }
    }

    @Override
    public void onRelease(@NotNull DragEvent event, double mouseX, double mouseY) {
        for (PackList packList : this.getPackLists()) {
            if (packList.isHovered()) {
                packList.drop(event, mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        DragEvent event = this.getDragged();
        if (event != null) {
            PackList source = event.target();
            boolean validDrop = false;

            if (this.isUnlocked()) {
                for (PackList list : this.getPackLists()) {
                    list.renderDroppableZone(guiGraphics, event, mouseX, mouseY, partialTick);
                    if (!validDrop && list.isMouseOver(mouseX, mouseY)) {
                        validDrop = source == list || source.canInteract(list);
                    }
                }
            }

            this.dragEventRenderer.renderDragEvent(event, guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.requestCursor(validDrop ? CursorsExtended.GRABBING : CursorTypes.NOT_ALLOWED);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        return this.isDraggingSelection() || super.keyPressed(keyEvent);
    }

    @Override
    public boolean charTyped(CharacterEvent charEvent) {
        return this.isDraggingSelection() || super.charTyped(charEvent);
    }
}
