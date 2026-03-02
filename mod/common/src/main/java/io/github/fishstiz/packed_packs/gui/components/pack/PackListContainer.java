package io.github.fishstiz.packed_packs.gui.components.pack;

import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuItemBuilder;
import io.github.fishstiz.fidgetz.gui.components.contextmenu.ContextMenuProvider;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.gui.model.PackListViewModel;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PackListContainer extends AbstractWidget implements ContextMenuProvider, ContainerEventHandler {
    private final List<GuiEventListener> children = new ArrayList<>(2);
    private final ScreenContext screenContext;
    private final PackListViewModel viewModel;
    private final PackList packList;
    private @Nullable FolderDialog folderDialog;
    private @Nullable GuiEventListener focused;
    private boolean dragging;

    public PackListContainer(ScreenContext screenContext, PackListViewModel viewModel) {
        super(0, 0, 0, 0, CommonComponents.EMPTY);
        this.screenContext = screenContext;
        this.viewModel = viewModel;
        this.packList = new PackList(screenContext, viewModel);
        this.children.add(this.packList);
    }

    public void refresh() {
        FolderDialog dialog = this.folderDialog;
        if (this.viewModel.isFolderOpened()) {
            if (dialog == null) {
                dialog = new FolderDialog(this.screenContext, this.viewModel.createFolderViewModel());
                dialog.setBoundingBox(this.packList);
                this.folderDialog = dialog;
                this.children.add(dialog);
            }
        } else {
            if (dialog != null) {
                dialog.setOpen(false);
                this.folderDialog = null;
                this.children.remove(dialog);
            }
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.packList.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.folderDialog != null) {
            this.folderDialog.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        if (this.folderDialog != null) {
            this.folderDialog.updateNarration(narrationElementOutput.nest());
        } else {
            this.packList.updateNarration(narrationElementOutput.nest());
        }
    }

    public PackListViewModel getViewModel() {
        return this.viewModel;
    }

    public PackList list() {
        return this.packList;
    }

    public @Nullable FolderDialog folder() {
        return this.folderDialog;
    }

    @Override
    public List<GuiEventListener> children() {
        return this.children;
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean isDragging) {
        this.dragging = isDragging;
    }

    @Override
    public @Nullable GuiEventListener getFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        this.focused = focused;
    }

    @Override
    public int getHeight() {
        return this.packList.getHeight();
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        this.packList.setHeight(height);
    }

    @Override
    public int getWidth() {
        return this.packList.getWidth();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        this.packList.setWidth(width);
    }

    @Override
    public int getX() {
        return this.packList.getX();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.packList.setX(x);
    }

    @Override
    public int getY() {
        return this.packList.getY();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.packList.setY(y);
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        this.packList.setSize(width, height);
    }

    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
        this.packList.setPosition(x, y);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
        return ContainerEventHandler.super.nextFocusPath(focusNavigationEvent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
        return ContainerEventHandler.super.mouseClicked(mouseButtonEvent, doubleClicked);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        return ContainerEventHandler.super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        return ContainerEventHandler.super.mouseDragged(mouseButtonEvent, dragX, dragY);
    }

    @Override
    public boolean isFocused() {
        return ContainerEventHandler.super.isFocused();
    }

    @Override
    public void buildItems(ContextMenuItemBuilder builder, int mouseX, int mouseY) {
        this.getChildAt(mouseX, mouseY)
                .filter(ContextMenuProvider.class::isInstance)
                .ifPresent(provider -> ((ContextMenuProvider) provider).buildItems(builder, mouseX, mouseY));
    }
}
