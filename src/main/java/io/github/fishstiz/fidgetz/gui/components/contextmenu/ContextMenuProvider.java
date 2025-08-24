package io.github.fishstiz.fidgetz.gui.components.contextmenu;

public interface ContextMenuProvider {
    void buildItems(MenuItemBuilder builder, int mouseX, int mouseY);

    default MenuItemBuilder buildItems(int mouseX, int mouseY) {
        MenuItemBuilder builder = new MenuItemBuilder();
        this.buildItems(builder, mouseX, mouseY);
        return builder;
    }
}
