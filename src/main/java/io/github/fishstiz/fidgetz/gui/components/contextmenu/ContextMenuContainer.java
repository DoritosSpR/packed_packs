package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import net.minecraft.client.gui.components.events.ContainerEventHandler;

public interface ContextMenuContainer extends ContextMenuProvider, ContainerEventHandler {
    @Override
    default void buildItems(MenuItemBuilder builder, int mouseX, int mouseY) {
        this.getChildAt(mouseX, mouseY)
                .filter(ContextMenuProvider.class::isInstance)
                .ifPresent(provider -> ((ContextMenuProvider) provider).buildItems(builder, mouseX, mouseY));
    }
}
