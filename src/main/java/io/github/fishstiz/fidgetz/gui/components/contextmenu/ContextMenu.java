package io.github.fishstiz.fidgetz.gui.components.contextmenu;

import io.github.fishstiz.fidgetz.gui.components.*;
import io.github.fishstiz.fidgetz.gui.renderables.ColoredRect;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.renderables.sprites.Sprite;
import io.github.fishstiz.fidgetz.gui.shapes.GuiRectangle;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.GuiUtil;
import io.github.fishstiz.fidgetz.util.ARGBColor;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.SPACING;

public class ContextMenu extends ToggleableDialog<LayoutWrapper<LinearLayout>> {
    static final BooleanSupplier DEFAULT_ACTIVE_SUPPLIER = () -> true;
    static final int DEFAULT_TEXT_INACTIVE_COLOR = Theme.GRAY_500.withAlpha(0.5f);
    static final int DEFAULT_BORDER_COLOR = Theme.GRAY_500.getARGB();
    static final int DEFAULT_BACKGROUND_COLOR = Theme.GRAY_800.getARGB();
    private static final int ITEM_HEIGHT = 20;
    private static final int MIN_WIDTH = 150;
    private static final int MENU_POINT_OFFSET = 1;
    private static final int DROP_SHADOW_SIZE = 16;
    private final Builder builder;
    private final List<ContextMenu> childMenus = new ArrayList<>();
    private final int borderColor;
    private final int softSeparatorColor;
    private final ContextMenu parentMenu;
    private Direction direction;
    private boolean forceOpen;

    protected ContextMenu(Builder builder) {
        super(builder);

        this.builder = builder;
        this.borderColor = builder.borderColor;
        this.softSeparatorColor = ARGBColor.withAlpha(this.borderColor, 0.15f);
        this.parentMenu = builder.parentMenu;
        this.direction = builder.direction;
        this.addListener(open -> {
            if (!open) {
                this.direction = this.builder.direction;
                this.forceOpen = false;
            }
        });
    }

    @Override
    protected void clearWidgets() {
        super.clearWidgets();
        this.visitChildren(ContextMenu::clearWidgets);
        this.childMenus.clear();
    }

    private <T extends LayoutElement> T addChild(T child) {
        return this.root().layout().addChild(child);
    }

    private ItemWidget<?> createItemWidget(MenuItem item) {
        switch (item) {
            case ParentMenuItem parentItem -> {
                ContextMenu childMenu = Builder.ofChild(this.builder, this).setDirection(this.direction).build();
                this.childMenus.add(childMenu);
                return new ParentItemWidget(this.root().getWidth(), parentItem, this, childMenu);
            }
            case RenderableMenuItem renderableMenuItem -> {
                return new CustomItemWidget(this.root().getWidth(), renderableMenuItem, this);
            }
            default -> {
                return new ItemWidget<>(this.root().getWidth(), item, this);
            }
        }
    }

    private void setItems(List<MenuItem> items) {
        this.clearWidgets();
        this.root().setLayout(emptyLayout());

        for (int i = 0; i < items.size(); i++) {
            MenuItem current = items.get(i);
            MenuItem next = (i + 1 < items.size()) ? items.get(i + 1) : null;

            if (current == MenuItem.SEPARATOR) {
                this.addRenderableWidget(this.addChild(new Separator(this, this.root().getWidth(), this.borderColor)));
                continue;
            }

            this.addRenderableWidget(this.addChild(this.createItemWidget(current)));

            if (current.shouldAutoSeparate() && next != null && next.shouldAutoSeparate()) {
                this.addRenderableWidget(this.addChild(new Separator(this, this.root().getWidth(), this.softSeparatorColor)));
            }
        }

        this.childMenus.forEach(this::prependWidget);
    }

    private void open(int x, int y, Direction direction, List<MenuItem> items) {
        if (items.isEmpty()) return;

        this.direction = direction;
        this.setItems(items);
        this.root().arrangeElements();
        this.root().setPosition(this.clampX(x), this.clampY(y));
        this.setOpen(true);
    }

    public void open(int x, int y, List<MenuItem> items) {
        this.open(x, y, this.builder.direction, items);
    }

    private int clampX(int x) {
        GuiRectangle bounds = this.getBoundingBox();
        this.direction = this.direction.next(this.screen, bounds, x);
        return this.direction.clamp(this.screen, bounds, x);
    }

    private int clampY(int y) {
        GuiRectangle bounds = this.getBoundingBox();
        y = y - MENU_POINT_OFFSET;
        return y + bounds.getHeight() > this.screen.height ? Math.max(0, this.screen.height - bounds.getHeight()) : y;
    }

    public int getItemHeight() {
        return ITEM_HEIGHT;
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        DrawUtil.renderDropShadow(guiGraphics, x, y, width, height, DROP_SHADOW_SIZE);
        super.renderBackground(guiGraphics, x, y, width, height, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderForeground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        guiGraphics.renderOutline(x, y, width, height, this.borderColor);

        for (ContextMenu childMenu : this.childMenus) {
            childMenu.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    public @Nullable ContextMenu getOpenedChildMenu() {
        for (ContextMenu child : this.childMenus) {
            if (child.isOpen()) return child;
        }
        return null;
    }

    private boolean isHoveredAtDirection(int mouseX, int mouseY) {
        return this.isOpen() && (this.isMouseOverBounds(mouseX, mouseY) || this.direction.isHovered(mouseX, mouseY, this.getBoundingBox()));
    }

    public void visitChildren(Consumer<ContextMenu> visitor) {
        for (ContextMenu child : this.childMenus) {
            visitor.accept(child);
            child.visitChildren(visitor);
        }
    }

    public void visitParents(Consumer<ContextMenu> visitor) {
        if (this.parentMenu != null) {
            visitor.accept(this.parentMenu);
            this.parentMenu.visitParents(visitor);
        }
    }

    private void closeCascade() {
        this.setOpen(false);
        this.visitParents(parent -> parent.setOpen(false));
    }

    private static LinearLayout emptyLayout() {
        return LinearLayout.vertical();
    }

    public static <S extends Screen & ToggleableDialogContainer> ContextMenu.Builder builder(S screen) {
        return new ContextMenu.Builder(screen, new LayoutWrapper<>(emptyLayout(), MIN_WIDTH, 0));
    }

    public static class Builder extends ToggleableDialog.Builder<LayoutWrapper<LinearLayout>, Builder> {
        protected Direction direction = Direction.RIGHT;
        protected int backgroundColor = DEFAULT_BACKGROUND_COLOR;
        protected int borderColor = DEFAULT_BORDER_COLOR;
        private ContextMenu parentMenu = null;

        protected <S extends Screen & ToggleableDialogContainer> Builder(S screen, LayoutWrapper<LinearLayout> root) {
            super(screen, root);
            this.focusOnOpen = false;
        }

        @SuppressWarnings("unchecked")
        protected static <S extends Screen & ToggleableDialogContainer> Builder ofChild(Builder builder, ContextMenu parentMenu) {
            Builder copy = builder((S) builder.screen);
            copy.backgroundColor = builder.backgroundColor;
            copy.borderColor = builder.borderColor;
            copy.background = builder.background;
            copy.autoClose = builder.autoClose;
            copy.focusOnOpen = builder.focusOnOpen;
            copy.autoLoseFocus = builder.autoLoseFocus;
            copy.parentMenu = parentMenu;
            return copy;
        }

        public Builder setBorderColor(int borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        @Override
        public Builder setBackground(int color) {
            this.backgroundColor = color;
            return this;
        }

        public Builder setDirection(Direction direction) {
            this.direction = direction;
            return this;
        }

        @Override
        public ContextMenu build() {
            if (this.background == null) {
                this.background = new ColoredRect(this.backgroundColor);
            }

            return new ContextMenu(this);
        }
    }

    private static class ItemWidget<T extends MenuItem> extends AbstractWidget {
        private static final int HOVER_OVERLAY_COLOR = Theme.WHITE.withAlpha(0.1f);
        private final FidgetzText<Void> text;
        private final Runnable onPress;
        protected final ContextMenu parent;
        protected final T item;

        private ItemWidget(int width, T item, ContextMenu parent) {
            super(0, 0, width, ITEM_HEIGHT, item.text());

            this.text = FidgetzText.<Void>builder().alignLeft().setMessage(item.text()).build();
            this.onPress = item.action();
            this.parent = parent;
            this.item = item;
        }

        @Override
        public void playDownSound(SoundManager handler) {
            if (this.item.active()) {
                super.playDownSound(handler);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (this.item.active()) {
                this.onPress.run();
            }
            this.parent.closeCascade();
        }

        protected void renderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
            RenderableRect background = this.item.background();
            if (background != null) {
                background.render(guiGraphics, x, y, width, height, partialTick);
            }
        }

        protected void renderIcon(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
            Sprite icon = this.item.icon();
            if (icon != null) {
                icon.render(guiGraphics, x, y, width, height, partialTick);
            }
        }

        protected void renderText(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
            this.text.setColor(this.item.textColor());
            this.text.setPosition(x, y);
            this.text.setSize(width, height);
            this.text.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }

        protected void renderHighlight(GuiGraphics guiGraphics, int x, int y, int right, int bottom, boolean hovered, float partialTick) {
            if (hovered && this.item.active()) {
                guiGraphics.fill(x, y, right, bottom, HOVER_OVERLAY_COLOR);
            }
        }

        @SuppressWarnings("unused")
        protected void renderForeground(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean hovered, double mouseX, double mouseY, float partialTick) {
            // for subclass
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);

            int x = this.getX();
            int y = this.getY();
            int width = this.getWidth();
            int height = this.getHeight();
            int right = x + width;
            int bottom = y + height;

            this.renderBackground(guiGraphics, x, y, width, height, partialTick);
            this.renderHighlight(guiGraphics, x, y, right, bottom, hovered, partialTick);

            int size = Minecraft.getInstance().font.lineHeight;
            int innerX = x + SPACING;
            int innerY = y + SPACING;
            int innerWidth = width - SPACING * 2;
            int innerHeight = height - SPACING * 2;
            int iconY = innerY + (innerHeight - size) / 2;
            int textX = this.item.icon() != null ? innerX + size + SPACING : innerX;
            int textWidth = this.item.icon() != null ? innerWidth - size - SPACING : innerWidth;

            this.renderIcon(guiGraphics, innerX, iconY, size, size, partialTick);
            this.renderText(guiGraphics, textX, innerY, textWidth, innerHeight, mouseX, mouseY, partialTick);
            this.renderForeground(guiGraphics, x, y, width, height, hovered, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            if (!this.parent.isOpen()) {
                return false;
            }
            for (ContextMenu siblingChild : this.parent.childMenus) {
                if (siblingChild.isOpen() && siblingChild.isMouseOverBounds(mouseX, mouseY)) {
                    return false;
                }
            }
            return super.isMouseOver(mouseX, mouseY);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            narrationElementOutput.add(NarratedElementType.TITLE, this.getMessage());
        }
    }

    private static class ParentItemWidget extends ItemWidget<ParentMenuItem> {
        private static final String CARET_RIGHT = ">";
        private final ContextMenu child;

        ParentItemWidget(int width, ParentMenuItem item, ContextMenu parent, ContextMenu child) {
            super(width, item, parent);
            this.child = child;
        }

        private void openChild() {
            GuiRectangle parentBounds = this.parent.getBoundingBox();
            GuiRectangle childBounds = this.child.getBoundingBox();
            Direction parentDirection = this.parent.direction;
            Direction nextDirection = parentDirection.next(parent.screen, childBounds, parentDirection.getX(parentBounds));
            int x = nextDirection.getX(parentBounds);
            int y = this.getY() + MENU_POINT_OFFSET;
            this.parent.visitChildren(menu -> {
                if (menu != this.child) menu.setOpen(false);
            });
            this.child.open(x, y, nextDirection, this.item.children());
        }

        private void closeChildren() {
            this.child.setOpen(false);
            this.child.visitChildren(menu -> menu.setOpen(false));
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (this.item.active()) {
                this.child.forceOpen = !this.child.forceOpen || !this.child.isOpen();
            }
            if (!this.child.isOpen()) {
                this.openChild();
            }
        }

        @Override
        protected void renderText(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
            int textWidth = width - (height + SPACING);
            super.renderText(guiGraphics, x, y, textWidth, height, mouseX, mouseY, partialTick);

            Font font = Minecraft.getInstance().font;
            int caretWidth = font.width(CARET_RIGHT);
            int caretHeight = font.lineHeight;
            int caretX = (x + textWidth + SPACING) + (height - caretWidth) / 2;
            int caretY = y + (height - caretHeight) / 2;
            int color = this.item.textColor();

            guiGraphics.drawString(font, CARET_RIGHT, caretX, caretY, color, false);
        }

        @Override
        protected void renderHighlight(GuiGraphics guiGraphics, int x, int y, int right, int bottom, boolean hovered, float partialTick) {
            super.renderHighlight(guiGraphics, x, y, right, bottom, hovered || this.child.isOpen(), partialTick);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

            if (!this.item.active()) {
                if (this.child.isOpen()) this.closeChildren();
                this.child.forceOpen = false;
                return;
            }

            ContextMenu sibling = this.parent.getOpenedChildMenu();
            if (!this.isHovered() && !this.child.forceOpen && (sibling == null || !sibling.isHoveredAtDirection(mouseX, mouseY))) {
                this.closeChildren();
            } else if (this.isHovered() && !this.child.isOpen() && (sibling == null || !sibling.forceOpen && !sibling.isHoveredAtDirection(mouseX, mouseY))) {
                this.openChild();
            }
        }
    }

    private static class CustomItemWidget extends ItemWidget<RenderableMenuItem> {
        private CustomItemWidget(int width, RenderableMenuItem item, ContextMenu parent) {
            super(width, item, parent);
        }

        @Override
        protected void renderForeground(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean hovered, double mouseX, double mouseY, float partialTick) {
            this.item.renderer().render(guiGraphics, x, y, width, height, partialTick);
        }
    }

    private static class Separator extends AbstractLayoutElement implements GuiEventListener, Renderable {
        private final ContextMenu parent;
        private final int color;

        private Separator(ContextMenu parent, int width, int color) {
            this.parent = parent;
            this.color = color;
            this.setSize(width, 1);
        }

        @Override
        public void setFocused(boolean focused) {
            // no-op
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public @NotNull ScreenRectangle getRectangle() {
            return super.getRectangle();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.parent.closeCascade();
            return false;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return GuiUtil.containsPoint(this, mouseX, mouseY);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.hLine(this.getX(), this.getRight() - 1, this.getMidY(), this.color);
        }
    }

    public enum Direction {
        LEFT {
            @Override
            protected Direction next(Screen screen, GuiRectangle bounds, int x) {
                return x - MENU_POINT_OFFSET - bounds.getWidth() / 2 < 0 ? RIGHT : this;
            }

            @Override
            protected int clamp(Screen screen, GuiRectangle bounds, int x) {
                return Math.max(0, x - MENU_POINT_OFFSET - bounds.getWidth());
            }

            @Override
            protected int getX(GuiRectangle bounds) {
                return bounds.getX();
            }

            @Override
            protected boolean isHovered(int mouseX, int mouseY, GuiRectangle bounds) {
                return mouseX >= bounds.getX() &&
                       mouseX <= bounds.getX() + bounds.getWidth() + HOVER_LEEWAY &&
                       mouseY >= bounds.getY() &&
                       mouseY <= bounds.getY() + bounds.getHeight();
            }
        },
        RIGHT {
            @Override
            protected Direction next(Screen screen, GuiRectangle bounds, int x) {
                return x + MENU_POINT_OFFSET + bounds.getWidth() / 2 > screen.width ? LEFT : this;
            }

            @Override
            protected int clamp(Screen screen, GuiRectangle bounds, int x) {
                int clamped = x + MENU_POINT_OFFSET;
                return clamped + bounds.getWidth() > screen.width ? screen.width - bounds.getWidth() : clamped;
            }

            @Override
            protected int getX(GuiRectangle bounds) {
                return bounds.getRight();
            }

            @Override
            protected boolean isHovered(int mouseX, int mouseY, GuiRectangle bounds) {
                return mouseX >= bounds.getX() - HOVER_LEEWAY &&
                       mouseX <= bounds.getX() + bounds.getWidth() &&
                       mouseY >= bounds.getY() &&
                       mouseY <= bounds.getY() + bounds.getHeight();
            }
        };

        private static final int HOVER_LEEWAY = 5;

        protected abstract Direction next(Screen screen, GuiRectangle bounds, int x);

        protected abstract int clamp(Screen screen, GuiRectangle bounds, int x);

        protected abstract int getX(GuiRectangle bounds);

        protected abstract boolean isHovered(int mouseX, int mouseY, GuiRectangle bounds);
    }
}
