package io.github.fishstiz.packed_packs.compat.respackopts;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.fishstiz.fidgetz.gui.components.ToggleableDialogContainer;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.PackWrapperDelegatorAbstractionEpicModelEntry;
import io.gitlab.jfronny.libjf.entrywidgets.api.v0.ResourcePackEntryWidget;
import io.gitlab.jfronny.respackopts.RespackoptsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

public class RespackoptsWidget extends AbstractButton {
    private final ResourcePackEntryWidget wrapped;
    private final PackSelectionModel.Entry model;
    private final LayoutElement container;

    private RespackoptsWidget(LayoutElement container, ResourcePackEntryWidget wrapped, PackSelectionModel.Entry model) {
        super(0, 0, 0, 0, Component.literal(Mod.RESPACKOPTS.getId()));

        this.container = container;
        this.wrapped = wrapped;
        this.model = model;
    }

    public static @Nullable RespackoptsWidget create(LayoutElement container, Pack pack) {
        for (ResourcePackEntryWidget widget : ResourcePackEntryWidget.WIDGETS) {
            PackSelectionModel.Entry model = new PackWrapperDelegatorAbstractionEpicModelEntry(pack);
            if (widget.isVisible(model, isSelectable(pack))) {
                return new RespackoptsWidget(container, widget, model);
            }
        }
        return null;
    }

    @Override
    public void onPress() {
        this.wrapped.onClick(this.model);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int width = this.wrapped.getWidth(this.model);
        int height = this.wrapped.getHeight(this.model, this.container.getHeight());
        int marginRight = this.wrapped.getXMargin(this.model);

        this.setWidth(width);
        this.setHeight(height);
        this.setX((this.container.getX() + this.container.getWidth()) - width - marginRight);
        this.setY(this.container.getY() + (this.container.getHeight() - height) / 2);

        this.isHovered = guiGraphics.containsPointInScissor(mouseX, mouseY) && this.isMouseOver(mouseX, mouseY);

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 1f);
        this.wrapped.render(this.model, guiGraphics, this.getX(), this.getY(), this.isHovered, partialTick);
        poseStack.popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // unsupported
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        boolean isMouseOver = super.isMouseOver(mouseX, mouseY);
        boolean isCoveredAtPoint = false;

        if (Minecraft.getInstance().screen instanceof ToggleableDialogContainer dialogContainer) {
            isCoveredAtPoint = dialogContainer.isChildCoveredAtPoint(this, mouseX, mouseY);
        }

        return isMouseOver && !isCoveredAtPoint;
    }

    public static boolean isForceReload() {
        return RespackoptsClient.forcePackReload;
    }

    /**
     * Copied from {@code TransferableSelectionList.Entry#showHoverOverlay()}
     */
    private static boolean isSelectable(Pack pack) {
        return !pack.isFixedPosition() || !pack.isRequired();
    }
}
