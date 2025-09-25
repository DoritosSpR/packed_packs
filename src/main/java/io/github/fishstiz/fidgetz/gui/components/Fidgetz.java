package io.github.fishstiz.fidgetz.gui.components;

import io.github.fishstiz.fidgetz.util.GuiUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;

public interface Fidgetz extends GuiEventListener, LayoutElement {
    private boolean isUncovered(double mouseX, double mouseY) {
        if (Minecraft.getInstance().screen instanceof ToggleableDialogContainer dialogContainer) {
            return !dialogContainer.isChildCoveredAtPoint(this, mouseX, mouseY);
        }
        return true;
    }

    @Override
    default boolean isMouseOver(double mouseX, double mouseY) {
        return GuiUtil.containsPoint(this, mouseX, mouseY) && this.isUncovered(mouseX, mouseY);
    }

    @Override
    default @NotNull ScreenRectangle getRectangle() {
        return LayoutElement.super.getRectangle();
    }
}
