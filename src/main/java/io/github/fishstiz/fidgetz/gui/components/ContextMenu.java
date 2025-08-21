package io.github.fishstiz.fidgetz.gui.components;

import com.google.common.base.Objects;
import com.google.common.util.concurrent.Runnables;
import io.github.fishstiz.fidgetz.gui.renderables.CenteredTextRect;
import io.github.fishstiz.fidgetz.gui.renderables.RenderableRect;
import io.github.fishstiz.fidgetz.gui.shapes.GuiRectangle;
import io.github.fishstiz.fidgetz.util.DrawUtil;
import io.github.fishstiz.fidgetz.util.ITheme;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import io.github.fishstiz.packed_packs.util.lang.ObjectsUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static io.github.fishstiz.packed_packs.util.constants.GuiConstants.SPACING;

public class ContextMenu extends ToggleableDialog<LayoutWrapper<LinearLayout>> {
    public static final int DEFAULT_BACKGROUND_COLOR = Theme.GRAY_800.getARGB();
    public static final int DEFAULT_BORDER_COLOR = Theme.GRAY_500.getARGB();
    public static final int MIN_WIDTH = 150;
    private static final int MENU_POINT_OFFSET = 1;
    private static final int DROP_SHADOW_SIZE = 16;
    private final Builder builder;
    private final List<ContextMenu> childMenus = new ArrayList<>();
    private final int borderColor;
    private final ContextMenu parentMenu;
    private Direction direction;

    protected ContextMenu(Builder builder) {
        super(builder);

        this.builder = builder;
        this.borderColor = builder.borderColor;
        this.parentMenu = builder.parentMenu;
        this.direction = builder.direction;
        this.addListener(open -> {
            if (!open) this.direction = this.builder.direction;
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

    private OptionWidget<?> createOptionWidget(Option current, Option next) {
        Integer separatorColor = ObjectsUtil.pick(ObjectsUtil.anyIdentity(next, null, Option.SEPARATOR), null, this.borderColor);
        if (current instanceof ParentOption parentOption) {
            ContextMenu childMenu = Builder.ofChild(this.builder, this).setDirection(this.direction).build();
            this.childMenus.add(this.prependWidget(childMenu));
            return new ParentOptionWidget(this.root().getWidth(), separatorColor, parentOption, this, childMenu);
        } else {
            return new OptionWidget<>(this.root().getWidth(), separatorColor, current, this);
        }
    }

    private void setOptions(Option... options) {
        this.clearWidgets();
        this.root().setLayout(emptyLayout());

        for (int i = 0; i < options.length; i++) {
            Option current = options[i];
            Option next = (i + 1 < options.length) ? options[i + 1] : null;

            if (current == Option.SEPARATOR) {
                this.addRenderableOnly(this.addChild(new Separator(this.root().getWidth(), this.borderColor)));
            } else {
                this.addRenderableWidget(this.addChild(this.createOptionWidget(current, next)));
            }
        }
    }

    private void open(int x, int y, Direction direction, Option... options) {
        if (options.length == 0) return;

        this.direction = direction;
        this.setOptions(options);
        this.root().arrangeElements();
        this.root().setPosition(this.clampX(x), this.clampY(y));
        this.setOpen(true);
    }

    public void open(int x, int y, Option... options) {
        this.open(x, y, this.builder.direction, options);
    }

    public void open(int x, int y, List<Option> options) {
        this.open(x, y, options.toArray(new Option[0]));
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

    @Override
    protected void renderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        DrawUtil.renderDropShadow(guiGraphics, x, y, width, height, DROP_SHADOW_SIZE);
        super.renderBackground(guiGraphics, x, y, width, height, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderForeground(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
        for (ContextMenu childMenu : this.childMenus) {
            childMenu.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private boolean isChildMenuHovered(int mouseX, int mouseY) {
        if (this.isOpen()) {
            for (ContextMenu child : this.childMenus) {
                if (child.isOpen()) {
                    if (child.isChildMenuHovered(mouseX, mouseY)) {
                        return true;
                    }
                    if (child.isMouseOverBounds(mouseX, mouseY) || child.direction.isHovered(mouseX, mouseY, child.getBoundingBox())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void visitChildren(Consumer<ContextMenu> visitor) {
        for (ContextMenu child : this.childMenus) {
            visitor.accept(child);
            child.visitChildren(visitor);
        }
    }

    public void visitParents(Consumer<ContextMenu> visitor) {
        visitor.accept(this);
        if (this.parentMenu != null) {
            this.parentMenu.visitParents(visitor);
        }
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
            if (this.background == null || this.background instanceof DefaultMenuBackground) {
                this.background = new DefaultMenuBackground(this.backgroundColor, this.borderColor);
            }

            return new ContextMenu(this);
        }
    }

    public interface Option {
        Option SEPARATOR = SimpleOption.newEmpty();

        Component text();

        Runnable onPress();
    }

    public record SimpleOption(Component text, Runnable onPress) implements Option {
        private static final SimpleOption EMPTY = newEmpty();

        public static SimpleOption newEmpty() {
            return new SimpleOption(Component.empty(), Runnables.doNothing());
        }

        public static SimpleOption empty() {
            return EMPTY;
        }
    }

    public record ParentOption(Component text, Option... children) implements Option {
        @Override
        public Runnable onPress() {
            return Runnables.doNothing();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || this.getClass() != o.getClass()) return false;
            ParentOption that = (ParentOption) o;
            return Objects.equal(this.text, that.text) && Objects.equal(this.children, that.children);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.text, Arrays.hashCode(this.children));
        }

        @Override
        public @NotNull String toString() {
            return "ParentOption{" +
                   "text=" + this.text +
                   ", children=" + Arrays.toString(this.children) +
                   '}';
        }
    }

    private record DefaultMenuBackground(int backgroundColor, int borderColor) implements RenderableRect {
        @Override
        public void render(GuiGraphics guiGraphics, int x, int y, int width, int height, float partialTick) {
            guiGraphics.fill(x, y, x + width, y + height, this.backgroundColor);
            guiGraphics.renderOutline(x, y, width, height, this.borderColor);
        }
    }

    private static class OptionWidget<T extends Option> extends AbstractWidget {
        private static final int HOVER_OVERLAY_COLOR = Theme.WHITE.withAlpha(0.1f);
        private static final int DEFAULT_HEIGHT = 20;
        private final FidgetzText<Void> text;
        private final Runnable onPress;
        private final Integer separator;
        protected final ContextMenu parent;
        protected final T option;

        private OptionWidget(int width, Integer separator, T option, ContextMenu parent) {
            super(0, 0, width, DEFAULT_HEIGHT, option.text());

            this.text = FidgetzText.<Void>builder().setMessage(option.text()).alignLeft().build();
            this.separator = ObjectsUtil.mapOrNull(separator, color -> ITheme.withAlpha(color, 0.15f));
            this.onPress = option.onPress();
            this.parent = parent;
            this.option = option;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.onPress.run();
            this.parent.visitParents(menu -> menu.setOpen(false));
        }

        protected void renderSeparator(GuiGraphics guiGraphics, int minX, int maxX, int y) {
            if (this.separator != null) {
                guiGraphics.hLine(minX, maxX, y, this.separator);
            }
        }

        protected void renderText(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
            this.text.setPosition(x, y);
            this.text.setSize(width, height);
            this.text.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int width = this.getWidth();
            int height = this.getHeight();
            int right = x + width;
            int bottom = y + height;

            if (this.isMouseOver(mouseX, mouseY)) guiGraphics.fill(x, y, right, bottom, HOVER_OVERLAY_COLOR);
            this.renderSeparator(guiGraphics, x, right - 1, bottom - 1);
            if (this.isFocused()) guiGraphics.renderOutline(x, y, width, height, Theme.WHITE.getARGB());

            int textX = x + SPACING;
            int textY = y + SPACING;
            int textWidth = width - SPACING * 2;
            int textHeight = height - SPACING * 2;

            this.renderText(guiGraphics, textX, textY, textWidth, textHeight, mouseX, mouseY, partialTick);
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

    private static class ParentOptionWidget extends OptionWidget<ParentOption> {
        private static final CenteredTextRect CARET_RIGHT = new CenteredTextRect(">", false);
        private final ContextMenu child;
        private boolean forcedOpen;

        ParentOptionWidget(int width, Integer separator, ParentOption option, ContextMenu parent, ContextMenu child) {
            super(width, separator, option, parent);
            this.child = child;
            this.forcedOpen = parent.isOpen();
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
            this.child.open(x, y, nextDirection, this.option.children);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.forcedOpen = !this.forcedOpen;
        }

        @Override
        protected void renderText(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
            int textWidth = width - (height + SPACING);
            super.renderText(guiGraphics, x, y, textWidth, height, mouseX, mouseY, partialTick);

            int caretX = x + textWidth + SPACING;
            //noinspection SuspiciousNameCombination
            CARET_RIGHT.render(guiGraphics, caretX, y, height, height, partialTick);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

            if (!this.forcedOpen && !this.isHovered() && !this.parent.isChildMenuHovered(mouseX, mouseY)) {
                this.child.setOpen(false);
                this.child.visitChildren(menu -> menu.setOpen(false));
            } else if (this.isHovered() && !this.child.isOpen() && !this.parent.isChildMenuHovered(mouseX, mouseY)) {
                this.openChild();
            }
        }
    }

    private static class Separator extends AbstractLayoutElement implements Renderable {
        private final int color;

        private Separator(int width, int color) {
            this.color = color;
            this.setSize(width, 1);
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
